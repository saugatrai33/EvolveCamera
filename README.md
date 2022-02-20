Simple camera app for android using camerax lib.
![1](https://user-images.githubusercontent.com/22369188/154834456-44027ee4-20e0-42cc-a7f4-1563cc7abea4.png)
![2](https://user-images.githubusercontent.com/22369188/154834460-f815b054-ed52-474b-84c6-ed53698380cb.png)

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

## Image format reference.
[https://developer.android.com/reference/android/graphics/ImageFormat] Supported image format depends upon device.
