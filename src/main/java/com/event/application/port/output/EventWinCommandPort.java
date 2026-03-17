package com.event.application.port.output;

import com.event.domain.entity.EventWinEntity;

public interface EventWinCommandPort {

    EventWinEntity save(EventWinEntity eventWin);
}

