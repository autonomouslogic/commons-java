package com.autonomouslogic.commons.updater.s3;

import java.time.Instant;
import lombok.Value;

@Value
public class S3Meta {
	String versionId;
	Instant lastModified;
}
