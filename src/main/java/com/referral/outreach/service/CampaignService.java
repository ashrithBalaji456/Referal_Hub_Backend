package com.referral.outreach.service;

import com.referral.outreach.dto.CampaignRequest;
import com.referral.outreach.dto.CampaignResponse;
import com.referral.outreach.dto.PreviewResponse;
import java.util.List;

public interface CampaignService {
    CampaignResponse createCampaign(CampaignRequest request);
    CampaignResponse updateCampaign(Long id, CampaignRequest request);
    CampaignResponse getCampaignById(Long id);
    List<CampaignResponse> getAllCampaigns();
    void deleteCampaign(Long id);
    CampaignResponse enableCampaign(Long id);
    CampaignResponse disableCampaign(Long id);
    PreviewResponse previewCampaignEmail(Long campaignId, Long recruiterId);
    void triggerCampaignManually(Long campaignId, Long recruiterId);
    int triggerCampaignBatch(Long campaignId, int limit);
}
