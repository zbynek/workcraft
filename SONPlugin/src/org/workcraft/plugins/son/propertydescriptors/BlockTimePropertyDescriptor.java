package org.workcraft.plugins.son.propertydescriptors;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.workcraft.gui.propertyeditor.PropertyDescriptor;
import org.workcraft.plugins.son.elements.Block;


public class BlockTimePropertyDescriptor implements PropertyDescriptor{
	private final Block b;

	public BlockTimePropertyDescriptor(Block b) {
		this.b = b;
	}

	@Override
	public String getName() {
		return "Duration";
	}

	@Override
	public Class<?> getType() {
		return String.class;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public boolean isCombinable() {
		return false;
	}

	@Override
	public boolean isTemplatable() {
		return false;
	}

	@Override
	public Object getValue() throws InvocationTargetException {
		return b.getDuration();
	}

	@Override
	public void setValue(Object value) throws InvocationTargetException {
		b.setDuration((String)value);
	}

	@Override
	public Map<? extends Object, String> getChoice() {
		return null;
	}

}
