package com.coniv.mait.global.component;

import com.coniv.mait.global.component.dto.EmailMessage;
import com.coniv.mait.global.component.dto.EmailSendResult;

public interface EmailSender {

	EmailSendResult send(EmailMessage message);
}
