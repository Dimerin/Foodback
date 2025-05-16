package unipi.msss.foodback.home.data
import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class EEGNetClassifier(
    context: Context,
    modelFilename: String = "eegnet_preproc.tflite"
) {
    companion object {
        private const val NUM_CLASSES = 5
    }

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, modelFilename))
    }

    /**
     * Load the model file from the assets folder.
     */
    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    /**
     * Convert the input data to a ByteBuffer.
     *
     * @param channelsData 2D array of floats, where each row is a channel and each column is a sample.
     * @return ByteBuffer containing the input data in the required format.
     */
    private fun makeInputBuffer(channelsData: Array<FloatArray>): ByteBuffer {
        val C = channelsData.size              // e.g. 6 or 1
        val T = channelsData[0].size           // e.g. 1000 or 250
        val numFloats = 1 * C * T * 1
        val buffer = ByteBuffer
            .allocateDirect(numFloats * 4)       // 4 bytes per float
            .order(ByteOrder.nativeOrder())
        // NHWC: loop over batch (1), over height (C), width (T), depth (1)
        for (c in 0 until C) {
            for (t in 0 until T) {
            buffer.putFloat(channelsData[c][t])
            }
        }
        buffer.rewind()
        return buffer
    }

    /**
     * Run inference.
     *
     * @param eegData Flattened EEG data: length = EEG_CHANS * EEG_SAMPLES,
     *                in channel-major order: [ch0_sample0, ch0_sample1…,
     *                ch1_sample0… ch5_sample999]
     * @param hrData  Flattened HR data: length = AUX_CHANNELS * AUX_SAMPLES
     * @param edaData Flattened EDA data: length = AUX_CHANNELS * AUX_SAMPLES
     *
     * @return softmax scores array of size [NUM_CLASSES]
     */
    fun classify(
        eegData: Array<FloatArray>,
        hrData: Array<FloatArray>,
        edaData: Array<FloatArray>
    ): FloatArray { 

        // Preparing EEG input data
        val eegInputData = makeInputBuffer(eegData)
        // Preparing HR input data
        val hrInputData = makeInputBuffer(hrData)
        // Preparing EDA input data
        val edaInputData = makeInputBuffer(edaData)

        // Prepare outputs
        val outputShape = intArrayOf(1, NUM_CLASSES)  // e.g. [1,5]
        val outputBuffer = ByteBuffer
        .allocateDirect(outputShape[0] * outputShape[1] * 4)
        .order(ByteOrder.nativeOrder())

        val inputs = arrayOf<Any>(eegInputData, hrInputData, edaInputData)
        val outputs = mapOf<Int, Any>(
            0 to outputBuffer
        )

        Log.i("TFLite", "EEG buffer capacity = ${eegInputData.capacity()}, expected = ${6*1000*4}")
        Log.i("TFLite", "HR buffer capacity  = ${hrInputData.capacity()}, expected = ${1*250*4}")
        Log.i("TFLite", "EDA buffer capacity = ${edaInputData.capacity()}, expected = ${1*250*4}")

        // Run
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // Read result
        outputBuffer.rewind()
        val scores = FloatArray(outputShape[1])  //5 softmax scores
        for (i in 0 until outputShape[1]) {
            scores[i] = outputBuffer.float
        }
        return scores
    }

    fun close() {
        interpreter.close()
    }
}
