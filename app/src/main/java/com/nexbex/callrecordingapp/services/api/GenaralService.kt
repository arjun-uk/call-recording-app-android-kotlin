package com.nexbex.callrecordingapp.services.api

import android.util.Log
import com.nexbex.callrecordingapp.network.RetrofitClient
import com.nexbex.callrecordingapp.utils.GeneralCallback
import com.nexbex.callrecordingapp.utils.Services
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class GeneralService {
    private val apiService = RetrofitClient.apiService

    fun uploadRecording(filePath: String, callback: GeneralCallback) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("GeneralService", "File does not exist.${file},${filePath}")
            return
        }
        val requestBody = file.asRequestBody("audio/3gpp".toMediaTypeOrNull())
        Log.d("GeneralService", "File exists.")
        Log.d("GeneralService", "File path: ${file.absolutePath}")
        Log.d("GeneralService", "File name: ${file.name}")
        Log.d("GeneralService", "File size: ${file.length()}")
        Log.d("GeneralService", "File last modified: ${file.lastModified()}")
        val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
        apiService.uploadRecording(multipartBody).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                callback.onSuccess(Services.REQ_ID_UPLOAD_RECORDING,response.body())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onError(100,"Upload failed with exception: ${t.message}")
            }
        })

    }
}

