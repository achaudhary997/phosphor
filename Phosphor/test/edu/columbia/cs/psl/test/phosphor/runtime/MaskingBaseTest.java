package edu.columbia.cs.psl.test.phosphor.runtime;

import org.junit.Test;

import java.lang.reflect.Field;

public abstract class MaskingBaseTest extends FieldHolderBaseTest {

    /* Sets the specified primitive or primitive array fields of the specified object to new values from the
     * supplier. Supplied values are tainted if tainted is true. */
    public abstract void setFields(Object obj, Field[] fields, boolean taint) throws Exception;

    /* Gets the specified primitive or primitive array fields of the specified object and checks that their values
     * are tainted if tainted is true and that they aren't tainted if tainted is false. */
    public abstract void checkFields(Object obj, Field[] fields, boolean tainted) throws Exception;

    /* Checks that using Field.set to set non-tainted primitive fields to tainted values results in the fields' values
     * being tainted. */
    @Test
    public void testSetTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holder = new PrimitiveHolder(false);
        setFields(holder, PrimitiveHolder.fields(), true);
        holder.checkFieldsAreTainted();
    }

    /* Checks that using Field.set to set tainted primitive fields to non-tainted values results in the fields' values
     * being non-tainted. */
    @Test
    public void testSetNonTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holder = new PrimitiveHolder(true);
        setFields(holder, PrimitiveHolder.fields(), false);
        holder.checkFieldsAreNotTainted();
    }

    /* Checks that using Field.set to set primitive array fields with non-tainted elements to arrays with tainted
     * elements results in the fields' arrays' elements being tainted. */
    @Test
    public void testSetTaintedPrimitiveArrayField() throws Exception {
        PrimitiveArrayHolder holder = new PrimitiveArrayHolder(false);
        setFields(holder, PrimitiveArrayHolder.fields(), true);
        holder.checkFieldsAreTainted();
    }

    /* Checks that using Field.set to set primitive array fields with tainted elements to arrays with non-tainted
     * elements results in the fields' arrays' elements being non-tainted. */
    @Test
    public void testSetNonTaintedPrimitiveArrayField() throws Exception {
        PrimitiveArrayHolder holder = new PrimitiveArrayHolder(true);
        setFields(holder, PrimitiveArrayHolder.fields(), false);
        holder.checkFieldsAreNotTainted();
    }

    /* Checks that using Field.set to set 2-D primitive array fields with non-tainted elements to arrays with tainted
     * elements results in the fields' arrays' elements being tainted. */
    @Test
    public void testSetTainted2DPrimitiveArrayField() throws Exception {
        Primitive2DArrayHolder holder = new Primitive2DArrayHolder(false);
        setFields(holder, Primitive2DArrayHolder.fields(), true);
        holder.checkFieldsAreTainted();
    }

    /* Checks that using Field.set to set 2-D primitive array fields with tainted elements to arrays with non-tainted
     * elements results in the fields' arrays' elements being non-tainted. */
    @Test
    public void testSetNonTainted2DPrimitiveArrayField() throws Exception {
        Primitive2DArrayHolder holder = new Primitive2DArrayHolder(true);
        setFields(holder, Primitive2DArrayHolder.fields(), false);
        holder.checkFieldsAreNotTainted();
    }

    /* Checks that using Field.get to get tainted primitive fields returns tainted primitives. */
    @Test
    public void testGetTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holder = new PrimitiveHolder(true);
        checkFields(holder, PrimitiveHolder.fields(), true);
    }

    /* Checks that using Field.get to get non-tainted primitive fields returns a non-tainted primitives. */
    @Test
    public void testGetNonTaintedPrimitiveField() throws Exception {
        PrimitiveHolder holder = new PrimitiveHolder(false);
        checkFields(holder, PrimitiveHolder.fields(), false);
    }

    /* Checks that using Field.get to get primitive array fields with tainted elements returns primitive arrays
     * with tainted elements. */
    @Test
    public void testGetTaintedPrimitiveArrayField() throws Exception {
        PrimitiveArrayHolder holder = new PrimitiveArrayHolder(true);
        checkFields(holder, PrimitiveArrayHolder.fields(), true);
    }

    /* Checks that using Field.get to get primitive array fields with non-tainted elements returns primitive arrays
     * with non-tainted elements. */
    @Test
    public void testGetNonTaintedPrimitiveArrayField() throws Exception {
        PrimitiveArrayHolder holder = new PrimitiveArrayHolder(false);
        checkFields(holder, PrimitiveArrayHolder.fields(), false);
    }
}
