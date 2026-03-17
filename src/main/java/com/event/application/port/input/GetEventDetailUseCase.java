package com.event.application.port.input;

import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.GetEventDetailQuery;

public interface GetEventDetailUseCase {

    EventDetailDto getEventDetail(GetEventDetailQuery query);
}

