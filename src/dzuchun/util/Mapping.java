package dzuchun.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class Mapping<T> implements SavedObject {
	protected String name;

	protected T object;

	public Mapping() {
	}

	public Mapping(String nameIn) {
		this.name = nameIn;
	}

	public String getName() {
		return new String(name);
	}

	@Override
	public void read(ObjectInputStream streamIn) throws IOException {
		try {
			this.name = (String) streamIn.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void save(ObjectOutputStream streamIn) throws IOException {
		streamIn.writeObject(name);
	}
}
