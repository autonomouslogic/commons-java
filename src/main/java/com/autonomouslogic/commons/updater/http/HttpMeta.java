package com.autonomouslogic.commons.updater.http;

import java.time.Instant;
import lombok.Value;

@Value
public class HttpMeta {
	Instant lastModified;
	String etag;
}
