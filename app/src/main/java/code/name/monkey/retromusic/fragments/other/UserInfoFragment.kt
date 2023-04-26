package code.name.monkey.retromusic.fragments.other

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import code.name.monkey.retromusic.Constants.USER_BANNER
import code.name.monkey.retromusic.Constants.USER_PROFILE
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentUserInfoBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.applyToolbar
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.fragments.LibraryViewModel
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.profileBannerOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.userProfileOptions
import code.name.monkey.retromusic.util.ImageUtil
import code.name.monkey.retromusic.util.PreferenceUtil.userName
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.File

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 300L
            scrimColor = Color.TRANSPARENT
        }
        _binding = FragmentUserInfoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyToolbar(binding.toolbar)

        binding.nameContainer.accentColor()
        binding.next.accentColor()
        binding.name.setText(userName)

        binding.userImage.setOnClickListener {
            showUserImageOptions()
        }

        binding.bannerImage.setOnClickListener {
            showBannerImageOptions()
        }

        binding.next.setOnClickListener {
            val nameString = binding.name.text.toString().trim { it <= ' ' }
            if (nameString.isEmpty()) {
                showToast(R.string.error_empty_name)
                return@setOnClickListener
            }
            userName = nameString
            findNavController().navigateUp()
        }

        loadProfile()
        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }
        libraryViewModel.getFabMargin().observe(viewLifecycleOwner) {
            binding.next.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = it
            }
        }
    }

    private fun showBannerImageOptions() {
        val list = requireContext().resources.getStringArray(R.array.image_settings_options)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Banner Image")
            .setItems(list) { _, which ->
                when (which) {
                    0 -> selectBannerImage()
                    1 -> {
                        val appDir = requireContext().filesDir
                        val file = File(appDir, USER_BANNER)
                        file.delete()
                        loadProfile()
                    }
                }
            }
            .setNegativeButton(/* textId = */ R.string.action_cancel, /* listener = */ null)
            .create()
            .show()
    }

    private fun showUserImageOptions() {
        val list = requireContext().resources.getStringArray(R.array.image_settings_options)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Profile Image")
            .setItems(list) { _, which ->
                when (which) {
                    0 -> pickNewPhoto()
                    1 -> {
                        val appDir = requireContext().filesDir
                        val file = File(appDir, USER_PROFILE)
                        file.delete()
                        loadProfile()
                    }
                }
            }
            .setNegativeButton(/* textId = */ R.string.action_cancel, /* listener = */ null)
            .create()
            .show()
    }

    private fun loadProfile() {
        binding.bannerImage.let {
            Glide.with(/* fragment = */ this)
                .load(RetroGlideExtension.getBannerModel())
                .profileBannerOptions(RetroGlideExtension.getBannerModel())
                .into(it)
        }
        Glide.with(/* fragment = */ this)
            .load(RetroGlideExtension.getUserModel())
            .userProfileOptions(RetroGlideExtension.getUserModel(), requireContext())
            .into(binding.userImage)
    }

    private fun selectBannerImage() {
        ImagePicker.with(fragment = this)
            .compress(maxSize = 1440)
            .provider(ImageProvider.GALLERY)
            .crop(x = 16f, y = 9f)
            .createIntent {
                startForBannerImageResult.launch(it)
            }
    }

    private fun pickNewPhoto() {
        ImagePicker.with(fragment = this)
            .provider(ImageProvider.GALLERY)
            .cropSquare()
            .compress(maxSize = 1440)
            .createIntent {
                startForProfileImageResult.launch(it)
            }
    }

    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            saveImage(result) { fileUri ->
                setAndSaveUserImage(fileUri)
            }
        }

    private val startForBannerImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            saveImage(result) { fileUri ->
                setAndSaveBannerImage(fileUri)
            }
        }

    private fun saveImage(
        result: ActivityResult,
        doIfResultOk: (uri: Uri) -> Unit,
    ) {
        val resultCode = result.resultCode
        val data = result.data
        when (resultCode) {
            Activity.RESULT_OK -> {
                data?.data?.let { uri ->
                    doIfResultOk(uri)
                }
            }

            ImagePicker.RESULT_ERROR -> {
                showToast(ImagePicker.getError(data))
            }

            else -> {
                showToast(message = "Task Cancelled")
            }
        }
    }

    private fun setAndSaveBannerImage(fileUri: Uri) {
        Glide.with(/* fragment = */ this)
            .asBitmap()
            .load(fileUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean,
                ): Boolean {
                    resource?.let { saveImage(it, USER_BANNER) }
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean,
                ): Boolean {
                    return false
                }
            })
            .into(binding.bannerImage)
    }

    private fun saveImage(bitmap: Bitmap, fileName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val appDir = requireContext().filesDir
            val file = File(appDir, fileName)
            var successful: Boolean
            file.outputStream().buffered().use {
                successful = ImageUtil.resizeBitmap(
                    /* src = */ bitmap,
                    /* maxForSmallerSize = */ 2048
                )
                    .compress(
                        /* format = */ Bitmap.CompressFormat.WEBP,
                        /* quality = */ 100,
                        /* stream = */ it
                    )
            }
            if (successful) {
                withContext(Dispatchers.Main) {
                    showToast(stringRes = R.string.message_updated)
                }
            }
        }
    }

    private fun setAndSaveUserImage(fileUri: Uri) {
        Glide.with(/* fragment = */ this)
            .asBitmap()
            .load(fileUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean,
                ): Boolean {
                    resource?.let { saveImage(it, USER_PROFILE) }
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean,
                ): Boolean {
                    return false
                }
            })
            .into(binding.userImage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
