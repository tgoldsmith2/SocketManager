package com.nimitz;

import java.io.IOException;

public interface CommandRunner {

	void runCommand(SocketCommand command) throws IOException;
}
