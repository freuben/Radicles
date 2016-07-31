+ TextView {

	addString {arg string;
		this.setString((string ++ ""), this.selectionStart);
	}

	backspace {
		this.setString("", this.selectionStart - 1, this.selectionStart);
	}
}