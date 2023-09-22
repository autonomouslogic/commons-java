package com.autonomouslogic.commons.updater.http;

import java.time.ZonedDateTime;
import lombok.Value;

@Value
public class HttpMeta {
	ZonedDateTime lastModified;
	String etag;
}
