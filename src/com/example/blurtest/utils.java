package com.example.blurtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;

public class utils {
	public static Bitmap fastblur(Bitmap sentBitmap, int radius) {

		// Stack Blur v1.0 from
		// http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
		//
		// Java Author: Mario Klingemann <mario at quasimondo.com>
		// http://incubator.quasimondo.com
		// created Feburary 29, 2004
		// Android port : Yahel Bouaziz <yahel at kayenko.com>
		// http://www.kayenko.com
		// ported april 5th, 2012

		// This is a compromise between Gaussian Blur and Box blur
		// It creates much better looking blurs than Box Blur, but is
		// 7x faster than my Gaussian Blur implementation.
		//
		// I called it Stack Blur because this describes best how this
		// filter works internally: it creates a kind of moving stack
		// of colors whilst scanning through the image. Thereby it
		// just has to add one new block of color to the right side
		// of the stack and remove the leftmost color. The remaining
		// colors on the topmost layer of the stack are either added on
		// or reduced by one, depending on if they are on the right or
		// on the left side of the stack.
		//
		// If you are using this algorithm in your code please add
		// the following line:
		//
		// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

		if (radius < 1) {
			return (null);
		}

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] pix = new int[w * h];
		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);

		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;

		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];

		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}

		yw = yi = 0;

		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;

		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;

			for (x = 0; x < w; x++) {

				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];

				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;

				sir = stack[i + radius];

				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];

				rbs = r1 - Math.abs(i);

				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;

				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}

				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
						| (dv[gsum] << 8) | dv[bsum];

				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;

				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];

				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];

				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];

				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];

				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];

				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;

				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];

				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];

				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];

				yi += w;
			}
		}

		Log.e("pix", w + " " + h + " " + pix.length);
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);

		return (bitmap);
	}

	public static Bitmap blur(Context ctx, Bitmap bkg) {

		float radius = 19;

		RenderScript rs = RenderScript.create(ctx);

		Allocation overlayAlloc = Allocation.createFromBitmap(rs, bkg);

		ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs,
				overlayAlloc.getElement());

		blur.setInput(overlayAlloc);

		blur.setRadius(radius);

		blur.forEach(overlayAlloc);

		overlayAlloc.copyTo(bkg);

		rs.destroy();
		return bkg;

	}

	public static Bitmap FitNewSize(Bitmap bitmap, float scale) {
		int new_w = (int) (bitmap.getWidth() * scale);
		int new_h = (int) (bitmap.getHeight() * scale);

		int[] pix = new int[bitmap.getWidth() * bitmap.getHeight()];
		bitmap.getPixels(pix, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),
				bitmap.getHeight());

		int[] new_pix = new int[new_w * new_h];
		int step = (int) (1 / scale);
		int c = step * step;
		for (int i = 0; i < new_h; i++) {
			for (int j = 0; j < new_w; j++) {

				int x = (int) (i / scale);
				int y = (int) (j / scale);

				int a = 0;
				int r = 0;
				int g = 0;
				int b = 0;

				int ww = Math.min(bitmap.getWidth(), x + step / 2);
				int hh = Math.min(bitmap.getWidth(), y + step / 2);
				for (int ii = Math.max(0, x - step / 2); ii < ww; ii++) {
					for (int jj = Math.max(0, y - step / 2); jj < hh; jj++) {
						int co = pix[ii * bitmap.getWidth() + jj];

						a += Color.alpha(co);
						r += Color.red(co);
						g += Color.green(co);
						b += Color.blue(co);
					}
				}

				if (c != 0) {
					a /= c;
					r /= c;
					g /= c;
					b /= c;
				}

				new_pix[i * new_w + j] = Color.argb(a, r, g, b);
			}
		}
		Bitmap b = Bitmap.createBitmap(new_w, new_h, Config.ARGB_8888);
		b.setPixels(new_pix, 0, new_w, 0, 0, new_w, new_h);
		return b;
	}

	public static Bitmap shrink(Bitmap b, float scale) {
		if (true) {
			int w = (int) (scale * b.getWidth());
			int h = (int) (scale * b.getHeight());
			Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);

			Canvas c = new Canvas(bitmap);

			c.drawBitmap(b, new Rect(0, 0, b.getWidth(), b.getHeight()),
					new RectF(0, 0, scale * b.getWidth(), h), null);
			return bitmap;
		} else {
			return utils.FitNewSize(b, scale);
		}
	}

	public static Bitmap getSnapshotBitmap(View v) {

		Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Config.ARGB_8888);

		Canvas c = new Canvas(bitmap);
		v.draw(c);

		return bitmap;
	}

	public static Bitmap getSubBitmap(Bitmap b, Rect rc) {

		Bitmap bitmap = Bitmap.createBitmap(rc.width(), rc.height(),
				Config.ARGB_8888);

		Canvas c = new Canvas(bitmap);
		c.drawBitmap(b, rc, new RectF(0, 0, rc.width(), rc.height()), null);
		return bitmap;
	}

}
