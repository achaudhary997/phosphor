package edu.columbia.cs.psl.phosphor.runtime;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.TaintUtils;
import edu.columbia.cs.psl.phosphor.struct.*;

import java.io.Serializable;

public class Taint<T> implements Serializable {

	public static boolean IGNORE_TAINTING;

	/* Returns a copy of the specified taint object. */
	public static <T> Taint<T> copyTaint(Taint<T> in) {
		return (in == null) ? null : in.copy();
	}

	/* Represents the set of labels for this taint object. May be the node representing the empty set. */
	private SetPool<Object>.SetNode labelSet;

	/* Constructs a new taint object with a null label set. */
	public Taint() {
		this.labelSet = SetPool.getInstance().emptySet();
	}

	/* Constructs a new taint object with only the specified label in its label set. */
	public Taint(T initialLabel) {
		if(initialLabel == null) {
			this.labelSet = SetPool.getInstance().emptySet();
		} else {
			this.labelSet = SetPool.getInstance().makeSingletonSet(initialLabel);
		}
	}

	/* Constructs a new taint object with the same labels as the specified taint object. */
	public Taint(Taint<T> t1) {
		if(t1 != null) {
			this.labelSet = t1.labelSet;
		} else {
			this.labelSet = SetPool.getInstance().emptySet();
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.singleDepCreated(t1, this);
		}
	}

	/* Constructs a new taint object whose label set is the union of the label sets of the two specified taint objects. */
	public Taint(Taint<T> t1, Taint<T> t2) {
		if(t1 != null && t2 != null) {
			this.labelSet = t1.labelSet.union(t2.labelSet);
		} else if(t1 != null) {
			this.labelSet = t1.labelSet;
		} else if(t2.labelSet != null) {
			this.labelSet = t2.labelSet;
		} else {
			this.labelSet = SetPool.getInstance().emptySet();
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.doubleDepCreated(t1, t2, this);
		}
	}

	/* Returns a copy of this taint object. */
	public Taint<T> copy() {
		if(IGNORE_TAINTING) {
			return this;
		} else {
			Taint<T> ret = new Taint<>();
			ret.labelSet = this.labelSet;
			return ret;
		}
	}

	/* Provides a formatted string representation of this taint object's labels. */
	@Override
	public String toString() {
		return "Taint [Labels = [" + labelSet.toList() + "]";
	}

	/* Returns a list containing this taint object's labels sorted in ascending order. */
	public LinkedList<Object> getLabels() {
		return labelSet.toList();
	}

	/* Sets this taint object's label set to be the union between this taint object's label set and the specified other
	 * taint object's label set. Returns whether this taint object's label set changed. */
	public boolean addDependency(Taint<T> other) {
		if(other == null) {
			return false;
		}
		SetPool<Object>.SetNode union = this.labelSet.union(other.labelSet);
		boolean changed = (this.labelSet != union);
		this.labelSet = union;
		return changed;
	}

	/* Returns whether this taint object's label set is the empty set. */
	public boolean isEmpty() {
		return labelSet.isEmpty();
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(TaintedBooleanWithObjTag ret) {
		ret.val = isEmpty();
		ret.taint = null;
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag isEmpty$$PHOSPHORTAGGED(ControlTaintTagStack ctrl, TaintedBooleanWithObjTag ret) {
		ret.val = isEmpty();
		ret.taint = null;
		return ret;
	}

	public static <T> void combineTagsOnArrayInPlace(Object[] ar, Taint<T>[] t1, int dims) {
		combineTagsInPlace(ar, t1[dims-1]);
		if(dims == 1) {
			for(Object o : ar) {
				combineTagsInPlace(o, t1[dims-1]);
			}
		}
		else {
			for(Object o : ar) {
				combineTagsOnArrayInPlace((Object[]) o, t1, dims-1);
			}
		}
	}

	public static <T> void combineTagsInPlace(Object obj, Taint<T> t1) {
		if(obj == null || t1 == null || IGNORE_TAINTING) {
			return;
		}
		_combineTagsInPlace(obj, t1);
	}

	@SuppressWarnings("unchecked")
	public static <T> void _combineTagsInPlace(Object obj, Taint<T> t1) {
		Taint<T> t = (Taint<T>) TaintUtils.getTaintObj(obj);
		if(t == null && t1 != null) {
			MultiTainter.taintedObject(obj, t1.copy());
		} else if(t != null && t1 != null) {
			t.addDependency(t1);
		}
	}

	public static <T> Taint<T> combineTags(Taint<T> t1, Taint<T> t2) {
		if(t1 == null && t2 == null) {
			return null;
		} else if(t2 == null || t2.isEmpty()) {
			return t1;
		} else if(t1 == null || t1.isEmpty()) {
			return t2;
		} else if(t1.equals(t2) || IGNORE_TAINTING) {
			return t1;
		} else if(t1.contains(t2)) {
			return t1;
		} else if(t2.contains(t1)) {
			return t2;
		} else {
			Taint<T> r = t1.copy();
			r.addDependency(t2);
			if(Configuration.derivedTaintListener != null) {
				Configuration.derivedTaintListener.doubleDepCreated(t1, t2, r);
			}
			return r;
		}
	}

	/* Returns whether the set of labels for the specified taint object is a subset of the set of labels for this taint
	 * object. */
	public boolean contains(Taint<T> that) {
		return that == null || this.labelSet.isSuperset(that.labelSet);
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = contains(that);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = contains(that);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag contains$$PHOSPHORTAGGED(Taint<T> that, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = contains(that);
		return ret;
	}

	/* Returns whether the set of labels for this taint object contains only the specified labels. */
	public boolean containsOnlyLabels(Object[] labels) {
		if(labels.length != getLabels().size()) {
			return false;
		}
		for(Object label : labels) {
			if(!containsLabel(label)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsOnlyLabels$$PHOSPHORTAGGED(Object[] labels, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = containsOnlyLabels(labels);
		return ret;
	}

	/* Returns whether the set of labels for this taint object contains the specified label. */
	public boolean containsLabel(Object label) {
		return labelSet.contains(label);
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithObjTag ret) {
		ret.taint = null;
		ret.val = containsLabel(label);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithIntTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithIntTag ret) {
		ret.taint = 0;
		ret.val = containsLabel(label);
		return ret;
	}

	@SuppressWarnings("unused")
	public TaintedBooleanWithObjTag containsLabel$$PHOSPHORTAGGED(Object label, TaintedBooleanWithObjTag ret, ControlTaintTagStack ctrl) {
		ret.taint = null;
		ret.val = containsLabel(label);
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		} else {
			Taint<?> taint = (Taint<?>) o;
			return taint.labelSet == this.labelSet;
		}
	}

	@Override
	public int hashCode() {
		return labelSet.hashCode();
	}

	@SuppressWarnings("unchecked")
	public static <T>  Taint<T> _combineTagsInternal(Taint<T> t1, ControlTaintTagStack tags) {
		if(t1 == null && tags.taint == null && (!Configuration.IMPLICIT_EXCEPTION_FLOW || (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty()))) {
			return null;
		}
		Taint tagsTaint;
		if(Configuration.IMPLICIT_EXCEPTION_FLOW) {
			if((tags.influenceExceptions == null || tags.influenceExceptions.isEmpty())) {
				//Can do a direct check of taint subsumption, no exception data to look at
				if(tags.getTag() == null)
					return t1;
				if(t1 == null)
					return tags.copyTag();
				if(t1.contains(tags.getTag()))
					return t1;
				if(tags.getTag().contains(t1))
					return tags.copyTag();
			}
			tagsTaint = tags.copyTagExceptions();
		} else {
			tagsTaint = tags.copyTag();
		}
		if(t1 == null || t1.isEmpty()) {
			return tagsTaint;
		} else if(tagsTaint == null || tagsTaint.isEmpty()) {
			return t1;
		} else if(t1 == tagsTaint) {
			return t1;
		}
		if(IGNORE_TAINTING) {
			return t1;
		}
		tagsTaint.addDependency(t1);
		return tagsTaint;
	}

	public static <T>  Taint<T> combineTags(Taint<T> t1, ControlTaintTagStack tags) {
		if(t1 == null && tags.taint == null && (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty())) {
			return null;
		}
		return _combineTagsInternal(t1,tags);
	}

	@SuppressWarnings("rawtypes")
	public static void combineTagsOnObject(Object o, ControlTaintTagStack tags) {
		if((tags.isEmpty() || IGNORE_TAINTING) && (!Configuration.IMPLICIT_EXCEPTION_FLOW || (tags.influenceExceptions == null || tags.influenceExceptions.isEmpty()))) {
			return;
		}
		if(Configuration.derivedTaintListener != null) {
			Configuration.derivedTaintListener.controlApplied(o, tags);
		}
		if(o instanceof String) {
			combineTagsOnString((String) o, tags);
		} else if(o instanceof TaintedWithObjTag) {
			((TaintedWithObjTag) o).setPHOSPHOR_TAG(Taint.combineTags((Taint) ((TaintedWithObjTag)o).getPHOSPHOR_TAG(), tags));
		}
	}

	private static void combineTagsOnString(String str, ControlTaintTagStack ctrl) {
		Taint existing = str.PHOSPHOR_TAG;
		str.PHOSPHOR_TAG = combineTags(existing, ctrl);

		LazyCharArrayObjTags tags = str.valuePHOSPHOR_TAG;
		if (tags == null) {
			str.valuePHOSPHOR_TAG = new LazyCharArrayObjTags(str.value);
			tags = str.valuePHOSPHOR_TAG;
		}
		if (tags.taints == null) {
			tags.taints = new Taint[str.length()];
		}
		for (int i = 0; i < tags.taints.length; i++) {
			tags.taints[i] = combineTags(tags.taints[i], ctrl);
		}
	}
}