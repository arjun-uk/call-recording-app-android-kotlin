package com.nexbex.callrecordingapp.utils

interface GeneralCallback {
    fun onSuccess(serviceID: Int, response: Any?)
    fun onError(serviceID: Int,error: String)
}