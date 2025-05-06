import argparse
import logging

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
        # Only first 6 EXG channels (exclude bias & reference)
        all_channels = BoardShim.get_exg_channels(self.board_id)
        self.exg_channels = all_channels[:6]
        self.sampling_rate = BoardShim.get_sampling_rate(self.board_id)
        self.update_speed_ms = 50
        self.window_size = 5
        self.num_points = self.window_size * self.sampling_rate

        self.app = QtGui.QApplication([])
        self.win = pg.GraphicsWindow(title='Mindrove Plot', size=(800, 600))

        self._init_pens()
        self._init_timeseries()
        self._init_psd()
        self._init_band_plot()

        timer = QtCore.QTimer()
        timer.timeout.connect(self.update)
        timer.start(self.update_speed_ms)
        QtGui.QApplication.instance().exec_()

    def _init_pens(self):
        self.pens = []
        self.brushes = []
        colors = ['#A54E4E', '#A473B6', '#5B45A4', '#2079D2', '#32B798',
                  '#2FA537', '#9DA52F', '#A57E2F', '#A53B2F']
        for col in colors:
            pen = pg.mkPen({'color': col, 'width': 2})
            brush = pg.mkBrush(col)
            self.pens.append(pen)
            self.brushes.append(brush)

    def _init_timeseries(self):
        self.plots, self.curves = [], []
        for i, ch in enumerate(self.exg_channels):
            p = self.win.addPlot(row=i, col=0)
            p.hideAxis('left'); p.hideAxis('bottom')
            if i == 0:
                p.setTitle('TimeSeries Plot')
            self.plots.append(p)
            curve = p.plot(pen=self.pens[i % len(self.pens)])
            self.curves.append(curve)

    def _init_psd(self):
        self.psd_plot = self.win.addPlot(row=0, col=1,
                                         rowspan=len(self.exg_channels)//2)
        self.psd_plot.setTitle('PSD Plot')
        self.psd_plot.setLogMode(False, True)
        self.psd_curves = []
        self.psd_size = DataFilter.get_nearest_power_of_two(self.sampling_rate)
        for i in range(len(self.exg_channels)):
            c = self.psd_plot.plot(pen=self.pens[i % len(self.pens)])
            c.setDownsampling(auto=True, method='mean', ds=3)
            self.psd_curves.append(c)
    
    def _init_band_plot(self):
        self.band_plot = self.win.addPlot(row=len(self.exg_channels)//2,
                                          col=1,
                                          rowspan=len(self.exg_channels)//2)
        # Show bottom axis for band labels
        self.band_plot.hideAxis('left')
        self.band_plot.showAxis('bottom')
        self.band_plot.setTitle('BandPower Plot')
        x = [1, 2, 3, 4, 5]
        self.band_bar = pg.BarGraphItem(x=x, height=[0]*5, width=0.8,
                                       pen=self.pens[0], brush=self.brushes[0])
        self.band_plot.addItem(self.band_bar)
        # Set captions under each bar
        labels = ['delta', 'theta', 'alpha', 'beta', 'gamma']
        ticks = [(i+1, labels[i]) for i in range(len(labels))]
        self.band_plot.getAxis('bottom').setTicks([ticks])

    def update(self):
        data = self.board_shim.get_current_board_data(self.num_points)
        band_sums = [0] * 5

        for idx, ch in enumerate(self.exg_channels):
            signal = data[ch]

            # 1) Detrend
            DataFilter.detrend(signal, DetrendOperations.CONSTANT.value)

            # 2) Band-pass 0.5-50 Hz (typical EEG range)
            DataFilter.perform_bandpass(signal, self.sampling_rate,
                                       0.5, 50.0, 2,
                                       FilterTypes.BUTTERWORTH.value, 0)

            # 3) Notch filter: narrow around main at 50 Hz (i.e.  power frequency noise)
            DataFilter.perform_bandstop(signal, self.sampling_rate,
                                        49.0, 51.0, 2,
                                        FilterTypes.BUTTERWORTH.value, 0)

            # Update time-series plot
            self.curves[idx].setData(signal.tolist())

            # PSD & band power
            if data.shape[1] > self.psd_size:
                # Tuple of Amplitude and Frequency arrays
                psd_data = DataFilter.get_psd_welch(
                    signal, self.psd_size, self.psd_size//2,
                    self.sampling_rate, WindowOperations.BLACKMAN_HARRIS.value
                )
                lim = min(70, len(psd_data[0]))
                self.psd_curves[idx].setData(psd_data[1][:lim].tolist(), psd_data[0][:lim].tolist())

                # Sum power in 5 bands
                band_sums[0] += DataFilter.get_band_power(psd_data, 1.0, 4.0)
                band_sums[1] += DataFilter.get_band_power(psd_data, 4.0, 8.0)
                band_sums[2] += DataFilter.get_band_power(psd_data, 8.0, 13.0)
                band_sums[3] += DataFilter.get_band_power(psd_data, 13.0, 30.0)
                band_sums[4] += DataFilter.get_band_power(psd_data, 30.0, 50.0)

        # Normalize across bands to get true percentage contributions
        total = sum(band_sums)
        if total > 0:
            percents = [int(100 * bs / total) for bs in band_sums]
        else:
            percents = [0] * 5

        self.band_bar.setOpts(height=percents)

        self.app.processEvents()


def main():
    BoardShim.enable_dev_board_logger()
    logging.basicConfig(level=logging.DEBUG)

    params = MindRoveInputParams()

    try:
        board_shim = BoardShim(BoardIds.MINDROVE_WIFI_BOARD, params)
        board_shim.prepare_session()
        board_shim.start_stream()
        Graph(board_shim)
    except BaseException:
        logging.warning('Exception', exc_info=True)
    finally:
        if board_shim.is_prepared():
            logging.info('Releasing session')
            board_shim.release_session()


if __name__ == '__main__':
    main()
