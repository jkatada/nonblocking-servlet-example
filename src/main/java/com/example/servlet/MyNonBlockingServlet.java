package com.example.servlet;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/nonblocking", asyncSupported = true)
public class MyNonBlockingServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		AsyncContext asyncContext = req.startAsync(req, resp);
		ServletInputStream input = req.getInputStream();
		ReadListener readListener = new ReadListenerImpl(input, resp, asyncContext);
		input.setReadListener(readListener);
	}
	//ommit
	
	public class ReadListenerImpl implements ReadListener {

		private final ServletInputStream input;
		private final HttpServletResponse resp;
		private final AsyncContext asyncContext;
		private StringBuilder sb = new StringBuilder();

		public ReadListenerImpl(ServletInputStream input, HttpServletResponse resp,
				AsyncContext asyncContext) {
			this.input = input;
			this.resp = resp;
			this.asyncContext = asyncContext;
		}

		@Override
		// データ読み込み可能になるとコールバックされる
		public void onDataAvailable() throws IOException {
			int len;
			byte[] b = new byte[1024];
			while (input.isReady() && !input.isFinished()
					&& (len = input.read(b)) != -1) {
				sb.append(new String(b, 0, len));
			}
		}

		@Override
		// 全データを読み終わるとコールバックされる
		public void onAllDataRead() throws IOException {
			ServletOutputStream output = resp.getOutputStream();
			WriteListener writeListener = new WriteListenerImpl(output, asyncContext, sb.toString());
			output.setWriteListener(writeListener);
		}

		@Override
		// エラー時にコールバックされる
		public void onError(Throwable throwable) {
			asyncContext.complete();
			throwable.printStackTrace();
		}

	}

	public class WriteListenerImpl implements WriteListener {
		private final ServletOutputStream output;
		private final AsyncContext asyncContext;
		private final String result;
		
		public WriteListenerImpl(ServletOutputStream output, AsyncContext asyncContext, String result) {
			this.output = output;
			this.asyncContext = asyncContext;
			this.result = result;
		}

		@Override
		// データ書き込み可能になるとコールバックされる
		public void onWritePossible() throws IOException {
			output.print("<body>" + result + "</body>");
			output.flush();
			asyncContext.complete();
		}

		@Override
		// エラー時にコールバックされる
		public void onError(Throwable throwable) {
			asyncContext.complete();
			throwable.printStackTrace();
		}
		
	}
}
