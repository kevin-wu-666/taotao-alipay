package test;

import static org.junit.Assert.*;

import org.junit.Test;

public class test {
	class A{
		private Integer a;

		public Integer getA() {
			return a;
		}

		public void setA(Integer a) {
			this.a = a;
		}

		public A(Integer a) {
			super();
			this.a = a;
		}
		
		
	}

	@Test
	public void test() {
		A a = new A(1);
		System.out.println(a.getA().equals(1));
	}

}
