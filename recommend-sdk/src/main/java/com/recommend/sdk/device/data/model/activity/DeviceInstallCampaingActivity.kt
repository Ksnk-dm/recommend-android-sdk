package com.recommend.sdk.device.data.model.activity

import com.recommend.sdk.device.data.model.activity.data.DeviceInstallCampaingActivityData

class DeviceInstallCampaingActivity(
    val userId: String?,
    val sessionId: String,
    val source: String?,
    val medium: String?,
    val campaign: String?,
    val campaignId: String?,
    val content: String?,
    val term: String?,
    val utmId: String?
) : BaseActivity {

    private val data = DeviceInstallCampaingActivityData(
        userId,
        sessionId,
        source,
        medium,
        campaign,
        campaignId,
        content,
        term,
        utmId
    )

    companion object {
        const val TYPE = "campaign_details"
    }

    override fun getType(): String {
        return TYPE
    }

    override fun getData(): Any? {
        return data
    }
}