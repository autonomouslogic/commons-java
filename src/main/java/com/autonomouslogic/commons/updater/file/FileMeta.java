package com.autonomouslogic.commons.updater.file;

import java.time.Instant;
import lombok.Value;

@Value
public class FileMeta {
	Instant lastModified;
}
