class Human {
	public String name;

	public Human(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

public class Basic{
	public static void main(String[] args) {
		Human abc = new Human("ABC");
		System.out.println(abc.getName());
		foo(2);
	}

	public static int foo(int a) {
		System.out.println("in foo()");
		return a + 2;
	}
}
