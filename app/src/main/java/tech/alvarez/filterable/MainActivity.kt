package tech.alvarez.filterable

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.huawei.hms.image.vision.ImageVision
import com.huawei.hms.image.vision.ImageVision.VisionCallBack
import com.huawei.hms.image.vision.ImageVisionImpl
import com.huawei.hms.image.vision.bean.ImageVisionResult
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tech.alvarez.filterable.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var imageVision: ImageVisionImpl

    var string =
        "{\"projectId\":\"projectIdTest\",\"appId\":\"appIdTest\",\"authApiKey\":\"authApiKeyTest\",\"clientSecret\":\"clientSecretTest\",\"clientId\":\"clientIdTest\",\"token\":\"tokenTest\"}"

    var filter: Int = 0
    var baseUri: Uri? = null
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.photoImageView.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            override fun onClick() {
                selectImageFromGallery()
            }

            override fun onSwipeRight() {
                filter = if (filter <= 0) 24 else filter - 1
                startFilter()
            }

            override fun onSwipeLeft() {
                filter = if (filter >= 24) 0 else filter + 1
                startFilter()
            }
        })

        binding.intensitySlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                startFilter()
            }
        })

        binding.compressRateSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                startFilter()
            }
        })

        binding.saveButton.setOnClickListener {
            binding.photoImageView.drawable?.let {
                val bitmap = (it as BitmapDrawable).bitmap
                saveBitmap(bitmap, "other")
                toast("Saved!")
            }
        }

        initImageVision()
    }

    private fun initImageVision() {
        val authJson = JSONObject(string)

        imageVision = ImageVision.getInstance(this)
        imageVision.setVisionCallBack(object : VisionCallBack {
            override fun onSuccess(successCode: Int) {
                val initCode = imageVision.init(this@MainActivity, authJson)
                log("Success Init: $successCode")
            }

            override fun onFailure(errorCode: Int) {
                log("Init Failed: $errorCode")
            }
        })
    }

    fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 666)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        baseUri = data?.data
        binding.photoImageView.setImageURI(baseUri)
        startFilter()
    }

    fun startFilter() {
        binding.photoImageView.setImageURI(baseUri)
        val intensity = binding.intensitySlider.value
        val compressRate = binding.compressRateSlider.value
        val bitmap = binding.photoImageView.drawable?.toBitmap()
        val config = prepareConfig(filter, intensity, compressRate)
        bitmap?.let { loadFilter(config, it) }
    }

    private fun prepareConfig(
        filterType: Int,
        intensity: Float = 1F,
        compress: Float = 1F
    ): JSONObject {
        val authJson = JSONObject(string)
        val taskJson = JSONObject()
        taskJson.put("intensity", intensity.toString())
        taskJson.put("filterType", filterType.toString())
        taskJson.put("compressRate", compress.toString())

        val config = JSONObject()
        config.put("requestId", "1")
        config.put("taskJson", taskJson)
        config.put("authJson", authJson)
        return config
    }

    private fun loadFilter(config: JSONObject, bitmap: Bitmap) {
        lifecycleScope.launch {
            val visionResult: ImageVisionResult = withContext(Dispatchers.Default) {
                imageVision.getColorFilter(config, bitmap)
            }
            binding.photoImageView.setImageBitmap(visionResult.image)
            binding.intensitySlider.value = 1F
            binding.compressRateSlider.value = 1F
            toast("${filters[filter]} ($filter)")
            log("Result: ${visionResult.response}, Code: ${visionResult.resultCode}")
        }
    }
}