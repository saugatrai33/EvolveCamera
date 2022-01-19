# EvolveCameraX
Initial release

![Screenshot_20211226-105603_EvolveCameraX](https://user-images.githubusercontent.com/22369188/147399535-0da7cf8d-9f9c-47f9-9392-4021f6ae33e8.jpg)
![Screenshot_20211226-105612_EvolveCameraX](https://user-images.githubusercontent.com/22369188/147399537-e0bd8455-3dfb-4bd3-8624-eb9fa4333071.jpg)


## Install with Gradle

```build.gradle``` project level

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
  
```build.gradle``` app level
```
implementation 'com.github.saugatrai33:EvolveCamera:2.0.6'
```
  
## Sample code
```
class MainActivity : AppCompatActivity() {

    private lateinit var image: ImageView

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val photoUri: Uri? = result.data?.data
            if (photoUri != null) {
                imageUri = photoUri
                showImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        image = findViewById(R.id.image)
        EvolveImagePicker.with(this)
            .start(evolveActivityResultLauncher)
    }
    
    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
		registerForActivityResult(
		    ActivityResultContracts.StartActivityForResult()
	) { result ->
	    val data = result.data!!.data
	    Log.d("MainActivity::", "result: ${data.toString()}")
	    Glide.with(this)
		.load(data)
		.apply(RequestOptions.centerCropTransform())
		.into(image)
	}
}
```

# With Fragment
```
EvolveImagePicker
	.with(requiredActivity())
        .start(evolveActivityResultLauncher)
```
  


# With Activity
```
EvolveImagePicker
	.with(this)
        .start(evolveActivityResultLauncher)
```

## Get the result as a uri
```
private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data!!.data
            Log.d("MainActivity::", "result: ${data.toString()}")
            Glide.with(this)
                .load(data)
                .apply(RequestOptions.centerCropTransform())
                .into(image)
        }
```

## IMAGE format reference.
[https://developer.android.com/reference/android/graphics/ImageFormat](Supported image format) depends upon device.
