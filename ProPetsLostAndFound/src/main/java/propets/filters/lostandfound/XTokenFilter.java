package propets.filters.lostandfound;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Order(10)
public class XTokenFilter implements Filter {
	

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String path = request.getServletPath();
		String method = request.getMethod();
		String xToken = request.getHeader("X-token");
		String url = "https://pro-pets-router.herokuapp.com/account/en/v1/check";
		
		if (!checkPointCut(path, method)) {
			if (xToken == null) {
				response.sendError(401);
				return;
			}
			RestTemplate restTemplate = new RestTemplate();
			URI urlAccServ = null;
			try {
				urlAccServ = new URI(url);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			HttpHeaders headers = new HttpHeaders();
			headers.add("X-token", xToken);
			RequestEntity<String> requestAccServ = new RequestEntity<>(headers, HttpMethod.GET, urlAccServ);
			ResponseEntity<String> responseAccServ/* = restTemplate.exchange(requestAccServ, String.class) */;
			try {
				responseAccServ = restTemplate.exchange(requestAccServ, String.class);
			} catch (RestClientException e) {
				response.sendError(409);
				return;
			}
			if (responseAccServ.getStatusCode().equals(HttpStatus.CONFLICT)) {
				response.sendError(409);
				return;
			}
			response.addHeader("X-token", responseAccServ.getHeaders().getFirst("X-token"));
			response.addHeader("X-userId", responseAccServ.getHeaders().getFirst("X-userId"));
			response.addHeader("X-userName", responseAccServ.getHeaders().getFirst("X-userName"));
			response.addHeader("X-avatar", responseAccServ.getHeaders().getFirst("X-avatar"));
			chain.doFilter(new WrapperRequest(request, responseAccServ.getHeaders().getFirst("X-userId")), response);
			return;
			
		}
		chain.doFilter(request, response);

	}

	private boolean checkPointCut(String path, String method) {
		boolean check = "Options".equalsIgnoreCase(method);
		return check;
	}
	
	private class WrapperRequest extends HttpServletRequestWrapper {
		String user;

		public WrapperRequest(HttpServletRequest request, String user) {
			super(request);
			this.user = user;
		}
		@Override
		public Principal getUserPrincipal() {
			return new Principal() { // () -> user;
				
				@Override
				public String getName() {
					return user;
				}
			};
		}
	}

}
