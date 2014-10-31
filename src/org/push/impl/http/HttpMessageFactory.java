package org.push.impl.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.push.protocol.Buffer;
import org.push.protocol.DeserializeData;
import org.push.protocol.IncomingPacket;
import org.push.protocol.MessageFactory;
import org.push.protocol.OutgoingPacket;
import org.push.protocol.ErrorCodes.DeserializeResult;
import org.push.protocol.ErrorCodes.SerializeResult;

public class HttpMessageFactory extends MessageFactory {

	public HttpMessageFactory() { }

	@Override
	public SerializeResult serializeMessage(OutgoingPacket outgoingPacket,
			Buffer buffer) {
		if (!(outgoingPacket instanceof HttpResponse)) {
			return SerializeResult.Failure;
		}
		
		String strLineSep = "\r\n";
		StringBuilder sb = new StringBuilder();
		
		HttpResponse httpResp = (HttpResponse)outgoingPacket; 
		
		// First Line
		sb.append(httpResp.getVersion()).append(" "); // http version
		sb.append(httpResp.getStatus().code()).append(" "); // status code
		sb.append(httpResp.getStatus().message()).append(" "); // message
		sb.append(strLineSep); // next line
		
		// Headers
		Map<String, String> headers = httpResp.getHeaders();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			sb.append(entry.getKey()).append(": ");
			sb.append(entry.getValue()).append(strLineSep);
		}
		sb.append(strLineSep); // end of headers

		String str = sb.toString();
		byte[] bytes = null;
		
		try {
			bytes = str.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			return SerializeResult.Failure;
		}
		
		if (!buffer.append(bytes)) {
			return SerializeResult.InsufficientBufferSpace;
		}
		
		// Entity
		byte[] entity = httpResp.getEntity().getBytes();
		if (entity != null && !buffer.append(entity)) {
			return SerializeResult.InsufficientBufferSpace;
		}

		return SerializeResult.Success;
	}

	@Override
	public DeserializeResult deserializeMessage(Buffer contentBytes,
			DeserializeData deserializeData) {
		byte[] content = contentBytes.getBuffer();
		int startIndex = 0;
		StringBuilder sbLine = new StringBuilder();
		
		try {
			startIndex = nextLine(content, startIndex, sbLine);
		} catch (HttpDecodingFailureException e) {
			return DeserializeResult.Failure;
		}
		
		// First Line
		String firstLine = sbLine.toString();
		if (firstLine.length() == 0) {
			return DeserializeResult.DiscardContent;
		}
		
		// <method> <url> <version>;
		String[] strs = firstLine.split(" ");
		if (strs.length != 3) {
			return DeserializeResult.Failure;
		}
		
		String method = strs[0].trim();
		String url = strs[1].trim();
		String version = strs[2].trim();
		
		// Headers
		String header = null;
		Map<String, String> headers = new HashMap<String, String>();
		int end = 0;
		do {
			sbLine.delete(0, sbLine.length());
			try {
				end = nextLine(content, startIndex, sbLine);
			} catch (HttpDecodingFailureException e) {
				return DeserializeResult.Failure;
			}
			
			// Line must be end with '\n'
			if (content[end - 1] != 13) {
				return DeserializeResult.Failure;
			}
			
			header = sbLine.toString();
			
			strs = header.split(":");
			if (strs.length != 2) {
				return DeserializeResult.Failure;
			}
			
			// save head name and value
			headers.put(strs[0].trim(), strs[1].trim());
			
			startIndex = end;
			
		} while (header != null && !"".equals(header));
		
		// Entity
		int entityLength = content.length - end;
		byte[] entity = null;
		if (entityLength > 0) {
			entity = new byte[entityLength];
		}
		
		System.arraycopy(content, end, entity, 0, entityLength);
		
		HttpRequest httpReq = new HttpRequest();
		
		httpReq.setMethod(method);
		httpReq.setUrl(url);
		httpReq.setVersion(version);
		httpReq.setHeaders(headers);
		
		HttpEntity entityObject = new HttpEntity();
		entityObject.setBytes(entity);
		httpReq.setEntity(entityObject);
		
		deserializeData.setMessage(httpReq);
		
		return DeserializeResult.Success;
	}
	
	private static int nextLine(byte[] bytes, int startIndex, 
			StringBuilder sb) throws HttpDecodingFailureException {
		if (bytes == null) {
			return 0;
		}

		int length = bytes.length;
		if (length == 0) {
			return 0;
		}

		int start = startIndex;
		// Start from 0
		if (start < 0) {
			start = 0;
		}
		
		byte b = 0;
		int i = start;
		for (; i < length; i ++) {
			b = bytes[i];
			if (b == 10) {
				int end = i;
				if (bytes[i - 1] == 13) {
					end --;
				}
				try {
					sb.append(new String(bytes, start, end - start, 
							"US-ASCII"));
				} catch (UnsupportedEncodingException e) {
					throw new HttpDecodingFailureException(e);
				}
				
				return end + 1;
			}
		}
		
		if (i > start) {
			try {
				sb.append(new String(bytes, start, i - start, 
						"US-ASCII"));
			} catch (UnsupportedEncodingException e) {
				throw new HttpDecodingFailureException(e);
			}
		}
		
		return i;
		
	}

	@Override
	public void disposeIncomingPacket(IncomingPacket packet) { }

	@Override
	public void disposeOutgoingPacket(OutgoingPacket packet) { }

}
