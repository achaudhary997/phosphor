package edu.columbia.cs.psl.test.phosphor;

import edu.columbia.cs.psl.phosphor.runtime.Taint;
import edu.columbia.cs.psl.phosphor.struct.LazyIntArrayObjTags;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ParameterAnnotationObjTagITCase {
	@Retention(RetentionPolicy.RUNTIME)
	@interface Annotated {
		public String value();
	}

	static class AnnotationHolder{
		public void primitiveMethod(String str, @Annotated(value="Param1") int param1, int noAnnotation2, @Annotated(value="param3") int[] param3){

		}
	}

	@Test
	public void testParameterAnnotationsAreReindexed() throws Exception{
		Method meth = AnnotationHolder.class.getDeclaredMethod("primitiveMethod$$PHOSPHORTAGGED",String.class, Taint.class, Integer.TYPE, Taint.class, Integer.TYPE, LazyIntArrayObjTags.class, int[].class);
		Annotation[][] annotations = meth.getParameterAnnotations();

		Assert.assertEquals(7, annotations.length);
		Assert.assertEquals(0, annotations[0].length);
		Assert.assertEquals(0, annotations[1].length);
		Assert.assertEquals(1, annotations[2].length);
		Annotated param1 = (Annotated) annotations[2][0];
		Assert.assertEquals("Param1",param1.value());
		Assert.assertEquals(0, annotations[3].length);
		Assert.assertEquals(0, annotations[4].length);
		Assert.assertEquals(0, annotations[5].length);
		Assert.assertEquals(1, annotations[6].length);
		Annotated param3 = (Annotated) annotations[6][0];
		Assert.assertEquals("param3",param3.value());

	}

}
