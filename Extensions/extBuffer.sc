+ Buffer {

		*cueSoundFileBuf { arg server, path, startFrame = 0, numChannels= 2, bufferSize=32768, completionMessage, bufnum;
		^this.alloc(server, bufferSize, numChannels, { arg buffer;
			buffer.readMsg(path, startFrame, bufferSize, 0, true, completionMessage)
		},  bufnum).cache
	}

}