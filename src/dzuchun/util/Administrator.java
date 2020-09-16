package dzuchun.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Administrator implements SavedObject {

	public Administrator() {
	}

	public Administrator(long id) {
		this.id = id;
	}

	private long id;

	public Long id() {
		return id;
	}

	@Override
	public void read(ObjectInputStream streamIn) throws IOException {
		this.id = streamIn.readLong();
	}

	@Override
	public void save(ObjectOutputStream streamIn) throws IOException {
		streamIn.writeLong(id);
	}

}
