package com.android.vadify.ui.login.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.android.vadify.R
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.databinding.FragmentPersonalInformationBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.login.fragment.popup.LocalLanguagePopUp
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.android.vadify.viewmodels.CommandsViewModel
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.fragment_personal_information.*
import kotlinx.coroutines.launch
import java.io.File


class PersonalInformationFragment : BaseDaggerFragment<FragmentPersonalInformationBinding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_personal_information
    var languageCode = ""
    var language = ""
    lateinit var languageCodeArray: List<String>

    val commandViewModel: CommandsViewModel by viewModels()

    companion object {
        private const val PROFILE_IMAGE_SIZE = 300
        const val LOCAL_FILE = "localLanguages.json"
    }

    val viewModel: ProfileFragmentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        viewModel.getLanguages()?.let {
//            val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, it)
//            mothertounge.setAdapter(arrayAdapter)
//        }

        /*SearchableDialog(requireActivity(),
            viewModel.getAllLanguageResponse(),
            "Select Language",
            {item, _ ->
                Toast.makeText(activity, item.title, Toast.LENGTH_SHORT).show()
            }).show()*/



        subscribe(cardView.throttleClicks()) {
            navigateCropImage()
        }

        subscribe(camera_image.throttleClicks()) {
            navigateCropImage()
        }

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        getAllLanguages()
        /* subscribe(mothertounge.throttleClicks()) {
             LocalLanguagePopUp {
                 viewModel.motherTounge.value = it.language
                 languageCode = it.code
                 language = it.code
             }.show(
                 childFragmentManager,
                 null
             )
         }*/

        viewModel.languageList.observe(requireActivity(), Observer {


            languageCodeArray = viewModel.languageList.value!!.map {
                it.code
            }

            var listName: MutableList<String> = ArrayList<String>()
            listName.add("Select Language")


            var listNameLanguage1 = viewModel.languageList.value!!.map {
                it.name.replace("(","( ") }
            // it.name


            /* val listName1 = viewModel.languageList.value!!.map {
                 it.name.replace("(","( ") }*/


            val listNameLanguage = listNameLanguage1!!.map {
                it.replace(")"," )") }


            listName.addAll(listNameLanguage)






            val adapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    requireActivity(),
                    android.R.layout.simple_spinner_item,
                    listName
                )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mothertounge.adapter = adapter
            mothertounge.setTitle("Select Language")


        })

        mothertounge.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapter: AdapterView<*>,
                view: View,
                position: Int,
                l: Long
            ) {
                //if (position > 0) {
                viewModel.motherTounge.value = adapter.getItemAtPosition(position).toString()
                if (position > 0) {
                    languageCode = languageCodeArray.get(position-1)
                    language = languageCodeArray.get(position-1)
                    Log.i(
                        "language-selected-",
                        viewModel.motherTounge.value.toString() + "code " + languageCode
                    )
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {

            }
        }


        viewModel.commandList.observe(requireActivity(), Observer { it ->
            // Log.d("commandGot: ", Gson().toJson(it))
            viewModel.puttlanguageCode(languageCode)
            if (!it.isEmpty()) {

                if (!it.filter { it1 -> it1.language.equals(viewModel.motherTounge.value) }
                        .isNullOrEmpty()) {
                    it.forEach {
                        val mCommandResust = Commands(
                            it.language,
                            it.commandName,
                            "",
                            it.command1?.toLowerCase(),
                            it.command2?.toLowerCase(),
                            it.command3?.toLowerCase()
                        )
                        commandViewModel.insert(mCommandResust)
                    }

                    val intent = Intent(requireContext(), Dashboard::class.java)
                    intent.putExtra(Dashboard.IS_FIRST_TIME, true)
                    startActivity(intent)
                    requireActivity().finish()
                } else {

                    findNavController().navigate(
                        PersonalInformationFragmentDirections.actionPersonalInformationFragmentToCommandsRegisterFragment(
                            true
                        )
                    )
                }
            } else {

                findNavController().navigate(
                    PersonalInformationFragmentDirections.actionPersonalInformationFragmentToCommandsRegisterFragment(
                        true
                    )
                )
            }


        })
        subscribe(StartBtn.throttleClicks()) {
            viewModel.puttlanguageCode(languageCode)
            viewModel.filterMethod { isCondition, message ->
                when {
                    isCondition -> {
                        bindNetworkState(
                            viewModel.updateProfileData(""),
                            progressDialog(R.string.updating)

                        ) {
                            // Log.d("update-profile: ", "" + Gson().toJson(it))

                            bindNetworkState(
                                viewModel.getLanguageUserResponse(),
                                progressDialog(R.string.updating)

                            )
                            //  hideKeyboard(StartBtn)
                            //progressDialog(R.string.updating)


                        }
                    }


                    else -> showSnackMessage(
                        resources.getString(
                            message ?: R.string.unknow_msg
                        )
                    )
                }
            }
        }


        subscribe(textView11.throttleClicks()) {
            startActivity(Intent(requireContext(), Dashboard::class.java))
            requireActivity().finish()


        }

    }

    private fun navigateCropImage() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropMenuCropButtonTitle(getString(R.string.Done))
            .setCropShape(CropImageView.CropShape.OVAL)
            .setAspectRatio(1, 1)
            .start(requireContext(), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        tryHandleCropImageResult(requestCode, resultCode, data) {
            lifecycleScope.launch {

                val listener = object : HandlePathOzListener.SingleUri {
                    override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
                        lifecycleScope.launch {
                            val file = File(pathOz.path)
                            try {
                                val compressedImageFile =
                                    Compressor.compress(requireContext(), file) {
                                        resolution(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE)
                                        format(Bitmap.CompressFormat.JPEG)
                                    }
                                bindNetworkState(
                                    viewModel.compressedImageFile(compressedImageFile),
                                    loadingIndicator = progressbar
                                )
                            } catch (e: Exception) {
                                print(e.stackTrace)
                            }
                        }
                    }

                }
                HandlePathOz(requireContext(), listener).getRealPath(it.uriContent!!)
            }
        }
    }

    override fun onBindView(binding: FragmentPersonalInformationBinding) {
        binding.vm = viewModel
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager =
            activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getAllLanguages() {
        bindNetworkState(
            viewModel.getAllLanguageResponseMo()
        )
    }

}
