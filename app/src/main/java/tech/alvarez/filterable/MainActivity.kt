package tech.alvarez.filterable

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.huawei.hms.image.vision.ImageVision
import com.huawei.hms.image.vision.ImageVision.VisionCallBack
import com.huawei.hms.image.vision.ImageVisionImpl
import com.huawei.hms.image.vision.bean.ImageVisionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tech.alvarez.filterable.databinding.ActivityMainBinding

val filters = arrayOf(
    "None",
    "Black-and-white",
    "Brown tone",
    "Lazy",
    "Freesia",
    "Fuji",
    "Peach pink",
    "Sea salt",
    "Mint",
    "Reed",
    "Vintage",
    "Marshmallow",
    "Moss",
    "Sunlight",
    "Time",
    "Haze blue",
    "Sunflower",
    "Hard",
    "Bronze yellow",
    "Monochromic tone",
    "Yellow-green tone",
    "Yellow tone",
    "Green tone",
    "Cyan tone",
    "Violet tone"
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var imageVision: ImageVisionImpl

    var filter: Int = 0
    var baseUri: Uri? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.apply {
            setContentView(root)
            photoImageView.setOnTouchListener(object :
                OnSwipeTouchListener(this@MainActivity) {
                override fun onClick() {
                    openGallery()
                }

                override fun onSwipeRight() {
                    filter = if (filter <= 0) 24 else filter - 1
                    startFilter(true)
                }

                override fun onSwipeLeft() {
                    filter = if (filter >= 24) 0 else filter + 1
                    startFilter(true)
                }
            })

            intensitySlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    startFilter()
                }
            })

            compressRateSlider.addOnSliderTouchListener(object :
                Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    startFilter()
                }
            })

            saveButton.setOnClickListener {
                photoImageView.drawable?.let {
                    val bitmap = (it as BitmapDrawable).bitmap
                    saveBitmap(bitmap, "photo")
                    toast("Saved!")
                }
            }
        }
        initImageVision()
    }

    private fun authJSON(): JSONObject {
        val authString =
            "{\"projectId\":\"projectIdTest\",\"appId\":\"appIdTest\",\"authApiKey\":\"authApiKeyTest\",\"clientSecret\":\"clientSecretTest\",\"clientId\":\"clientIdTest\",\"token\":\"tokenTest\"}"
        return JSONObject(authString)
    }

    private fun initImageVision() {
        imageVision = ImageVision.getInstance(this)
        imageVision.setVisionCallBack(object : VisionCallBack {
            override fun onSuccess(successCode: Int) {
                val initCode = imageVision.init(this@MainActivity, authJSON())
                log("Success Init: $successCode, Init Code: $initCode")
            }

            override fun onFailure(errorCode: Int) {
                log("Init Failed: $errorCode")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.data?.let {
            baseUri = it
            binding.intensitySlider.value = 1F
            binding.compressRateSlider.value = 1F
            binding.photoImageView.setImageURI(baseUri)
        }
    }

    fun startFilter(resetOptions: Boolean = false) {
        baseUri?.let { base ->
            val intensity = binding.intensitySlider.value
            val compressRate = binding.compressRateSlider.value
            val bitmap = base.bitmap(this)
            val config = prepareConfig(filter, intensity, compressRate)
            bitmap?.let { loadFilter(config, it, resetOptions) }
        }
    }

    private fun prepareConfig(
        filterType: Int,
        intensity: Float = 1F,
        compress: Float = 1F
    ): JSONObject {
        val taskJson = JSONObject()
        taskJson.put("intensity", intensity.toString())
        taskJson.put("filterType", filterType.toString())
        taskJson.put("compressRate", compress.toString())

        val config = JSONObject()
        config.put("requestId", "1")
        config.put("taskJson", taskJson)
        config.put("authJson", authJSON())
        return config
    }

    private fun loadFilter(config: JSONObject, bitmap: Bitmap, resetOptions: Boolean = false) {
        lifecycleScope.launch {
            val visionResult: ImageVisionResult = withContext(Dispatchers.Default) {
                imageVision.getColorFilter(config, bitmap)
            }
            binding.photoImageView.setImageBitmap(visionResult.image)
            if (resetOptions) {
                binding.intensitySlider.value = 1F
                binding.compressRateSlider.value = 1F
                toast(filters[filter])
            }
            log("Result: ${visionResult.response}, Code: ${visionResult.resultCode}")
        }
    }
}