import argparse
import logging

import numpy as np
import scipy.signal as sp_signal

import pyqtgraph as pg
from pyqtgraph.Qt import QtGui, QtCore

from mindrove.board_shim import BoardShim, MindRoveInputParams, BoardIds
from mindrove.data_filter import DataFilter, FilterTypes, WindowOperations, DetrendOperations


class Graph:
    def __init__(self, board_shim):
        pg.setConfigOption('background', 'w')
        pg.setConfigOption('foreground', 'k')

        self.board_id = board_shim.get_board_id()
        self.board_shim = board_shim
        all_ch = BoardShim.get_exg_channels(self.board_id)
        self.exg_channels = all_ch[:6]
        self.sampling_rate = BoardShim.get_sampling_rate(self.board_id)
        self.update_speed_ms = 50
        self.window_size = 5
        self.num_points = self.window_size * self.sampling_rate

        # Qt application and main window
        self.app = QtGui.QApplication([])
        self.win = pg.GraphicsLayoutWidget(title='Mindrove Plot', size=(1200, 600))
        self.win.show()  # Ensure window is displayed

        # Build plots
        self._init_pens()
        self._init_timeseries()
        self._init_psd()
        self._init_alt_psd()
        self._init_band_plot()

        # Timer for updates
        timer = QtCore.QTimer()
        timer.timeout.connect(self.update)
        timer.start(self.update_speed_ms)

        # Enter Qt event loop
        QtGui.QApplication.instance().exec_()

    def _init_pens(self):
        self.pens = []
        self.brushes = []
        colors = ['#A54E4E', '#A473B6', '#5B45A4', '#2079D2', '#32B798',
                  '#2FA537', '#9DA52F', '#A57E2F', '#A53B2F']
        for col in colors:
            self.pens.append(pg.mkPen(color=col, width=2))
            self.brushes.append(pg.mkBrush(col))
        self.alt_pen = pg.mkPen(color='k', width=1, style=QtCore.Qt.DashLine)

    def _init_timeseries(self):
        self.plots, self.curves = [], []
        for i, ch in enumerate(self.exg_channels):
            p = self.win.addPlot(row=i, col=0)
            p.hideAxis('left'); p.hideAxis('bottom')
            if i == 0: p.setTitle('TimeSeries')
            self.plots.append(p)
            self.curves.append(p.plot(pen=self.pens[i]))

    def _init_psd(self):
        self.psd_plot = self.win.addPlot(row=0, col=1, rowspan=len(self.exg_channels)//2)
        self.psd_plot.setTitle('Orig PSD')
        self.psd_plot.setLogMode(False, True)
        self.psd_curves = []
        self.psd_size = DataFilter.get_nearest_power_of_two(self.sampling_rate)
        for i in range(len(self.exg_channels)):
            c = self.psd_plot.plot(pen=self.pens[i]); c.setDownsampling(auto=True, method='mean', ds=3)
            self.psd_curves.append(c)

    def _init_alt_psd(self):
        self.alt_psd_plot = self.win.addPlot(row=0, col=2, rowspan=len(self.exg_channels)//2)
        self.alt_psd_plot.setTitle('Alt PSD')
        self.alt_psd_plot.setLogMode(False, True)
        self.alt_psd_curves = []
        for i in range(len(self.exg_channels)):
            c = self.alt_psd_plot.plot(pen=self.alt_pen); c.setDownsampling(auto=True, method='mean', ds=3)
            self.alt_psd_curves.append(c)

    def _init_band_plot(self):
        self.band_plot = self.win.addPlot(row=len(self.exg_channels)//2, col=1, colspan=2)
        self.band_plot.hideAxis('left'); self.band_plot.showAxis('bottom')
        self.band_plot.setTitle('Band Powers (%)')
        x = [1,2,3,4,5]
        self.band_bar = pg.BarGraphItem(x=x, height=[0]*5, width=0.8,
                                       pen=self.pens[0], brush=self.brushes[0])
        self.band_plot.addItem(self.band_bar)
        labels = ['delta','theta','alpha','beta','gamma']
        ticks = [(i+1, labels[i]) for i in range(5)]
        self.band_plot.getAxis('bottom').setTicks([ticks])

    def update(self):
        data = self.board_shim.get_current_board_data(self.num_points)
        band_sums = [0]*5

        raw = np.array([data[ch] for ch in self.exg_channels])
        raw_dt = sp_signal.detrend(raw, axis=1)
        b50, a50 = sp_signal.iirnotch(50, Q=30, fs=self.sampling_rate)
        raw_nt = sp_signal.filtfilt(b50, a50, raw_dt, axis=1)
        b_bp, a_bp = sp_signal.butter(2, [1,70], btype='band', fs=self.sampling_rate)
        raw_alt = sp_signal.filtfilt(b_bp, a_bp, raw_nt, axis=1)

        for idx, ch in enumerate(self.exg_channels):
            sig = data[ch].copy()
            DataFilter.detrend(sig, DetrendOperations.CONSTANT.value)
            DataFilter.perform_bandstop(sig, self.sampling_rate, 49,51,2, FilterTypes.BUTTERWORTH.value,0)
            DataFilter.perform_bandstop(sig, self.sampling_rate, 59,61,2, FilterTypes.BUTTERWORTH.value,0)
            DataFilter.perform_bandpass(sig, self.sampling_rate,1,70,2, FilterTypes.BUTTERWORTH.value,0)
            self.curves[idx].setData(sig.tolist())

            # PSD orig
            if sig.size >= self.psd_size:
                amps1, freqs1 = DataFilter.get_psd_welch(sig, self.psd_size, self.psd_size//2,
                                                        self.sampling_rate, WindowOperations.BLACKMAN_HARRIS.value)
                lim = min(70, len(freqs1))
                self.psd_curves[idx].setData(freqs1[:lim].tolist(), amps1[:lim].tolist())

                # Alt PSD
                alt_sig = raw_alt[idx]
                if alt_sig.size >= self.psd_size:
                    freqs2, psd2 = sp_signal.welch(alt_sig, fs=self.sampling_rate,
                                                   nperseg=self.psd_size, noverlap=self.psd_size//2,
                                                   window='blackmanharris')
                    lim2 = min(70, len(freqs2))
                    self.alt_psd_curves[idx].setData(freqs2[:lim2].tolist(), psd2[:lim2].tolist())

                for b, (fl, fh) in enumerate([(1,4),(4,8),(8,13),(13,30),(30,70)]):
                    band_sums[b] += DataFilter.get_band_power((amps1, freqs1), fl, fh)

        total = sum(band_sums)
        perc = [int(100*x/total) if total>0 else 0 for x in band_sums]
        self.band_bar.setOpts(height=perc)
        self.app.processEvents()


def main():
    BoardShim.enable_dev_board_logger()
    logging.basicConfig(level=logging.DEBUG)
    params = MindRoveInputParams()
    try:
        board_shim = BoardShim(BoardIds.MINDROVE_WIFI_BOARD, params)
        board_shim.prepare_session(); board_shim.start_stream()
        Graph(board_shim)
    except BaseException:
        logging.warning('Exception', exc_info=True)
    finally:
        if board_shim.is_prepared(): board_shim.release_session()

if __name__ == '__main__': main()