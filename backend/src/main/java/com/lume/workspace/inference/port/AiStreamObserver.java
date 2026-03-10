package com.lume.workspace.inference.port;

import com.lume.workspace.inference.orchestration.AiStreamEvent;

public interface AiStreamObserver {

    void onEvent(AiStreamEvent event);

    void onComplete();

    void onError(Throwable error);
}
