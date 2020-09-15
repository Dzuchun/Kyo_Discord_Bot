package dzuchun.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SavedArrayList<T> extends ArrayList<T> {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("Coords");

	private static final long serialVersionUID = 1L;

	public File saveFile;
	protected Function<ObjectInputStream, T> readFunction;
	protected BiConsumer<ObjectOutputStream, T> writeFunction;

	public SavedArrayList(File saveFileIn, Function<ObjectInputStream, T> readFunctionIn,
			BiConsumer<ObjectOutputStream, T> writeFunctionIn) {
		super();
		this.saveFile = saveFileIn;
		this.readFunction = readFunctionIn;
		this.writeFunction = writeFunctionIn;
	}

	public void save() throws IOException {
		FileOutputStream saver = new FileOutputStream(this.saveFile);
		ObjectOutputStream saverStream = new ObjectOutputStream(saver);
		saverStream.writeInt(this.size());
		this.forEach(t -> this.writeFunction.accept(saverStream, t));
		saverStream.close();
	}

	public void load() throws IOException {
		if (this.saveFile.createNewFile()) {
			return;
		}
		ObjectInputStream reader = null;
		try {
			reader = new ObjectInputStream(new FileInputStream(this.saveFile));
			int size = reader.readInt();
			for (int i = 0; i < size; i++) {
				this.add(this.readFunction.apply(reader));
			}
			reader.close();
		} catch (EOFException e) {
			if (reader != null) {
				reader.close();
			}
			e.printStackTrace();
		}
	}

	public boolean anyMatches(Predicate<T> condition) {
		boolean res = false;
		int size = this.size();
		for (int i = 0; i < size; i++) {
			if (condition.test(this.get(i))) {
				res = true;
				break;
			}
		}
		return res;
	}

	public T getMatches(Predicate<T> condition) {
		T res = null;
		int size = this.size();
		for (int i = 0; i < size; i++) {
			if (condition.test(this.get(i))) {
				res = this.get(i);
				break;
			}
		}
		return res;
	}

	@Override
	public String toString() {
		final StringBuilder res = new StringBuilder();
		this.forEach(t -> res.append(t.toString()).append(", "));
		return res.substring(0, Math.max(0, res.length() - 3));
	}
}
