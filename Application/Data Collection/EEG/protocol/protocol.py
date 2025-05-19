import os
import sys
import time
import threading
import argparse
import pandas as pd

os.environ['PYGAME_HIDE_SUPPORT_PROMPT'] = "hide"
import pygame

from mindrove.board_shim import BoardShim, MindRoveInputParams, BoardIds
from pyqtgraph.Qt import QtGui, QtCore
import pyqtgraph as pg


# ---------- Protocol Parameters ----------
TIME_TO_BRING_FOOD_TO_MOUTH = 5   # seconds
TIME_TO_TASTE_FOOD          = 10  # seconds

# ---------- Configuration ----------
AUDIO_FILE        = os.path.join('sounds', 'beep.mp3')
CSV_FILE          = 'eeg_tasting_raw_data.csv'
EEG_CHANNEL_COUNT = 6                   # first 6 EXG channels (the other 2 are the bias and reference channels)
RECORD_DURATION   = TIME_TO_TASTE_FOOD  # duration (in seconds) of EEG recording
PLOT_UPDATE_MS    = 50                  # redraw every 50 ms

# ---------- Audio Setup ----------
pygame.mixer.init()
_BEEP = pygame.mixer.Sound(AUDIO_FILE)

def play_beep():
    """Play beep.mp3 non-blocking via pygame."""
    _BEEP.play()

# ---------- Real-time Plotting Thread ----------
class PlotThread(threading.Thread):
    def __init__(self, board_shim):
        super().__init__(daemon=True)
        self.board_shim = board_shim
        self.board_id   = board_shim.get_board_id()
        self.exg_chs    = BoardShim.get_exg_channels(self.board_id)[:EEG_CHANNEL_COUNT]
        self.fs         = BoardShim.get_sampling_rate(self.board_id)
        self.window_size = 5  # seconds of the window to display
        
        print(f"Board ID: {self.board_id}")
        print(f"EEG channels: {self.exg_chs}")
        print(f"Sampling rate: {self.fs} Hz")

    def run(self):
        # set up Qt + Plot
        self.app = QtGui.QApplication([])
        self.win = pg.GraphicsLayoutWidget(title='Real-time EEG')
        self.win.resize(800, 600)
        self.curves = []
        for i, ch in enumerate(self.exg_chs):
            p = self.win.addPlot(row=i, col=0)
            p.hideAxis('left')
            p.hideAxis('bottom')
            if i == 0:
                p.setTitle('EEG Time Series')
            self.curves.append(p.plot())
        self.win.show()

        # timer to fetch & draw
        self.timer = QtCore.QTimer()
        self.timer.timeout.connect(self._update)
        self.timer.start(PLOT_UPDATE_MS)

        # start event loop (blocks this thread)
        self.app.exec_()

    def _update(self):
        # number of points
        npts = int(self.fs * self.window_size)
        # fetch data and update curves
        data = self.board_shim.get_current_board_data(npts)
        for curve, ch in zip(self.curves, self.exg_chs):
            curve.setData(data[ch].tolist())

    def stop(self):
        # schedule quit() in the Qt event loop
        QtCore.QTimer.singleShot(0, self.app.quit)


# ---------- Main Data Collection Routine ----------
def main():
    # --- Parse command-line ---
    parser = argparse.ArgumentParser(
        description="EEG tasting protocol (with optional save)."
    )
    parser.add_argument(
        "--collect_data",
        required=True,
        choices=["true", "false"],
        help="If true, append to CSV; if false, run protocol without saving for testing the protocol.",
    )
    args = parser.parse_args()
    save_data = args.collect_data.lower() == "true"

    # --- Determine experiment number ---
    if save_data:
        if os.path.exists(CSV_FILE):
            # Read only the experiment_number column to find max
            prev = pd.read_csv(CSV_FILE, usecols=['experiment_number'])
            exp_num = int(prev['experiment_number'].max() + 1)
        else:
            exp_num = 1
    else:
        exp_num = None  # not used in dry-run

    # Init board
    BoardShim.enable_dev_board_logger()
    params     = MindRoveInputParams()
    board_shim = BoardShim(BoardIds.MINDROVE_WIFI_BOARD, params)

    try:
        board_shim.prepare_session()
        board_shim.start_stream()
        
        # --- EEG Cap Connection Check ---
        time.sleep(1)
        arrived = board_shim.get_board_data_count()
        print(f"EEG samples received: {arrived}")
        if arrived < 0.5 * board_shim.get_sampling_rate(BoardIds.MINDROVE_WIFI_BOARD):
            board_shim.release_session()
            print("ERROR: Too few EEG samples received. Check Cap connection and try again.")
            sys.exit(1)

        # Launch real-time plot in its own thread
        plot_thread = PlotThread(board_shim)
        plot_thread.start()

        time.sleep(1)

        # Prompt for subject name and surname (to identify the subject)
        subject = ""
        while True:
            subject = input(
                "\nEnter subject NameSurname (camel case, e.g. SteveRogers): "
            ).strip()
            if subject and subject[0].isupper() and subject.isalnum():
                break
            print("  >> Please use CamelCase, no spaces or special chars.")

        # Wait for user to be ready
        input("\nPress ENTER to begin the tasting protocol...")

        # Protocol cues
        play_beep()                # 1st beep
        print(f"The subject has {TIME_TO_BRING_FOOD_TO_MOUTH} seconds to bring food to mouth.")
        time.sleep(TIME_TO_BRING_FOOD_TO_MOUTH)

        play_beep()                # 2nd beep → start recording
        print(f"EEG recording started.")
        print(f"The subject has {TIME_TO_TASTE_FOOD} seconds to taste the food.")
        t0 = time.time()           # UNIX timestamp
        time.sleep(TIME_TO_TASTE_FOOD)

        play_beep()                # 3rd beep → stop
        print(f"EEG recording stopped.")
        # fetch exactly RECORD_DURATION seconds
        sample_count = int(board_shim.get_sampling_rate(BoardIds.MINDROVE_WIFI_BOARD)
                           * RECORD_DURATION)
        raw = board_shim.get_current_board_data(sample_count)

        # Build DataFrame: UNIX timestamps + channels + subject
        timestamps = [
            t0 + i / board_shim.get_sampling_rate(BoardIds.MINDROVE_WIFI_BOARD)
            for i in range(sample_count)
        ]
        df = pd.DataFrame({"timestamp": timestamps})
        for idx, ch in enumerate(plot_thread.exg_chs):
            df[f"ch{idx}"] = raw[ch]
        df["subject"] = subject
        df["experiment_number"] = exp_num

        # Tear down plot & board
        plot_thread.stop()
        board_shim.stop_stream()
        board_shim.release_session()

        # Prompt for rating
        while True:
            try:
                rating = int(input("Enter level of appreciation (1-5): "))
                if 1 <= rating <= 5:
                    break
            except ValueError:
                pass
            print("Please enter an integer between 1 and 5.")
        df["rating"] = rating

        # Save or skip
        if save_data:
            write_header = not os.path.exists(CSV_FILE)
            df.to_csv(CSV_FILE, mode="a", index=False, header=write_header)
            print(f"\n[Experiment {exp_num}] Saved {len(df)} samples (rating={rating}) for {subject} to {CSV_FILE}.")
        else:
            print(f"\nTest run complete — data not saved (collect_data=false).")

    except Exception as e:
        print("Error during session:", e)
    finally:
        if board_shim.is_prepared():
            board_shim.release_session()

if __name__ == "__main__":
    main()
