package com.nexbex.callrecordingapp.network

import com.nexbex.callrecordingapp.utils.Services
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiInterface {
    @Multipart
    @POST(Services.URL_UPLOAD_RECORDING)
    fun uploadRecording(@Part file: MultipartBody.Part): Call<ResponseBody>
}