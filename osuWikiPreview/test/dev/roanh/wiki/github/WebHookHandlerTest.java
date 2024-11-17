package dev.roanh.wiki.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WebHookHandlerTest{

	
	
	
	
	
	
	
	
	
	
	@Test
	public void signatureValidationValid(){
		assertTrue(new WebHookHandler("It's a Secret to Everybody").validateSignature("757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17", "Hello, World!"));
	}
	
	@Test
	public void signatureValidationInvalid(){
		assertFalse(new WebHookHandler("It's not a Secret to Everybody").validateSignature("757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17", "Hello, World!"));
	}
}
