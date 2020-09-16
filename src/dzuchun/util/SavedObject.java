package dzuchun.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface SavedObject {

	public abstract void read(ObjectInputStream streamIn) throws IOException;

	public abstract void save(ObjectOutputStream streamIn) throws IOException;
}
