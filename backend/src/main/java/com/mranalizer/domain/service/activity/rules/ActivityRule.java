package com.mranalizer.domain.service.activity.rules;

import com.mranalizer.domain.model.MergeRequest;
import com.mranalizer.domain.model.activity.ActivityFlag;
import com.mranalizer.domain.port.out.activity.ReviewProvider.ReviewInfo;

import java.util.List;

public interface ActivityRule {

    List<ActivityFlag> evaluate(MergeRequest mr, List<ReviewInfo> reviews);
}
