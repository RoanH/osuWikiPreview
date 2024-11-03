/*
 * osu! wiki preview site
 * Copyright (C) 2023  Roan Hofland (roan@roanh.dev) and contributors.
 * GitHub Repository: https://github.com/RoanH/osuWikiPreview
 * GitLab Repository: https://git.roanh.dev/roan/osuwikipreview
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.roanh.wiki.github;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import dev.roanh.infinity.io.netty.http.HttpBody;
import dev.roanh.infinity.io.netty.http.WebServer;
import dev.roanh.infinity.io.netty.http.handler.BodyHandler;
import dev.roanh.infinity.io.netty.http.handler.RequestHandler;

public class WebHookHandler implements BodyHandler{
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private final Key secret;
	
	public WebHookHandler(String secret){
		this.secret = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
	}
	
	
	
	public static void init(){
		WebServer server = new WebServer(0);
		
		server.createContext(null, false, null);
		
		
		
	}

	@Override
	public FullHttpResponse handle(FullHttpRequest request, HttpBody data) throws Exception{
		String payload = data.string();
		if(!validateSignature(payload, request.headers())){
			return RequestHandler.status(HttpResponseStatus.FORBIDDEN);
		}
		
		
		
		
		
//		new Gson().fromJson(signatureHeader, null)
		
		
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException{
		//TODO unit test
		new WebHookHandler("It's a Secret to Everybody").validateSignature("757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17", "Hello, World!");
	}
	
	
	
	private final boolean validateSignature(String payload, HttpHeaders headers){
		String signatureHeader = headers.get("X-Hub-Signature-256");
		return signatureHeader != null && signatureHeader.length() == 64 + 7 && signatureHeader.startsWith("sha256=") && validateSignature(signatureHeader.substring(7), payload);
	}
	
	private final boolean validateSignature(String signature, String payload){
		try{
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secret);
			byte[] hexSignature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			
			int diff = 0;
			for(int i = 0; i < hexSignature.length; i++){
				diff |= ((HexFormat.fromHexDigit(signature.charAt(i * 2)) << 4) | HexFormat.fromHexDigit(signature.charAt(i * 2 + 1))) ^ (hexSignature[i] & 0xFF);
			}
			
			return diff == 0;
		}catch(NoSuchAlgorithmException | InvalidKeyException ignore){
			return false;
		}
	}
}
