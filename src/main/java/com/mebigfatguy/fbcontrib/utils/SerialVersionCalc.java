/*
 * fb-contrib - Auxiliary detectors for Java programs
 * Copyright (C) 2005-2018 Dave Brosius
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.mebigfatguy.fbcontrib.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class SerialVersionCalc {

	public static long uuid(JavaClass cls) throws IOException {

		if (cls.isEnum()) {
			return 0;
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");

			digest.update(cls.getClassName().getBytes(StandardCharsets.UTF_8));
			digest.update(toArray(cls.getModifiers()));

			String[] infs = cls.getInterfaceNames();
			Arrays.sort(infs);
			Arrays.stream(infs).forEach(inf -> digest.update(inf.getBytes(StandardCharsets.UTF_8)));

			Field[] fields = cls.getFields();
			Arrays.sort(fields, new FieldSorter());
			Arrays.stream(fields).filter(field -> !field.isPrivate() || (!field.isStatic() && !field.isTransient()))
					.forEach(field -> {
						digest.update(field.getName().getBytes(StandardCharsets.UTF_8));
						digest.update(toArray(field.getModifiers()));
						digest.update(field.getSignature().getBytes(StandardCharsets.UTF_8));
					});

			Method[] methods = cls.getMethods();
			Arrays.sort(methods, new MethodSorter());

			Arrays.stream(methods).filter(method -> "<clinit>".equals(method.getName())).forEach(sinit -> {
				digest.update(sinit.getName().getBytes(StandardCharsets.UTF_8));
				digest.update(toArray(sinit.getModifiers()));
				digest.update(sinit.getSignature().getBytes(StandardCharsets.UTF_8));
			});

			Arrays.stream(methods).filter(method -> "<init>".equals(method.getName()) && !method.isPrivate())
					.forEach(init -> {
						digest.update(init.getName().getBytes(StandardCharsets.UTF_8));
						digest.update(toArray(init.getModifiers()));
						digest.update(init.getSignature().getBytes(StandardCharsets.UTF_8));
					});

			Arrays.stream(methods).filter(method -> !"<init>".equals(method.getName()) && !method.isPrivate())
					.forEach(cons -> {
						digest.update(cons.getName().getBytes(StandardCharsets.UTF_8));
						digest.update(toArray(cons.getModifiers()));
						digest.update(cons.getSignature().getBytes(StandardCharsets.UTF_8));
					});

			byte[] sha = digest.digest();

			return (sha[0] & 0x00FF) | (sha[1] & 0x00FF) << 8 | (sha[2] & 0x00FF) << 16 | (sha[3] & 0x00FF) << 24
					| (sha[4] & 0x00FF) << 32 | (sha[5] & 0x00FF) << 40 | (sha[6] & 0x00FF) << 48
					| (sha[7] & 0x00FF) << 56;

		} catch (NoSuchAlgorithmException e) {
			return 0;
		}
	}

	private static byte[] toArray(int i) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(i);
		return b.array();
	}

	static class FieldSorter implements Comparator<Field> {

		@Override
		public int compare(Field f1, Field f2) {
			return f1.getName().compareTo(f2.getName());
		}
	}

	static class MethodSorter implements Comparator<Method> {

		@Override
		public int compare(Method m1, Method m2) {
			int cmp = m1.getName().compareTo(m2.getName());
			if (cmp != 0) {
				return cmp;
			}

			return m1.getSignature().compareTo(m2.getSignature());
		}
	}
}
