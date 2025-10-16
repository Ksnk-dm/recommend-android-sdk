package com.recommend.sdk.device.data.model.activity.data

data class DeviceInstallCampaingActivityData(
    val userId: String?,
    val sessionId: String,
    val source: String?,
    val medium: String?,
    val campaign: String?,
    val campaignId: String?,
    val content: String?,
    val term: String?,
    val utmId: String?
)