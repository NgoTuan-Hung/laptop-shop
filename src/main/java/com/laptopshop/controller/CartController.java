package com.laptopshop.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.laptopshop.entities.ChiMucGioHang;
import com.laptopshop.entities.GioHang;
import com.laptopshop.entities.NguoiDung;
import com.laptopshop.entities.SanPham;
import com.laptopshop.service.ChiMucGioHangService;
import com.laptopshop.service.GioHangService;
import com.laptopshop.service.NguoiDungService;
import com.laptopshop.service.SanPhamService;


@Controller
@SessionAttributes("loggedInUser")
public class CartController {
	
	@Autowired
	private SanPhamService sanPhamService;
	@Autowired
	private NguoiDungService nguoiDungService;
	@Autowired
	private GioHangService gioHangService;
	@Autowired
	private ChiMucGioHangService chiMucGioHangService;
	
	@ModelAttribute("loggedInUser")
	public NguoiDung loggedInUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return nguoiDungService.findByEmail(auth.getName());
	}
	
	public NguoiDung getSessionUser(HttpServletRequest request) {
		return (NguoiDung) request.getSession().getAttribute("loggedInUser");
	}
	
	@GetMapping("/cart")
	public String cartPage(HttpServletRequest req, HttpServletResponse res,Model model) {
		NguoiDung currentUser = getSessionUser(req);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Map<Long,String> quanity = new HashMap<Long,String>();
		List<SanPham> listsp = new ArrayList<SanPham>();
		Cookie cl[] = req.getCookies();

		if(auth == null || auth.getPrincipal() == "anonymousUser")     //Lay tu cookie
		{
			
			Set<Long> idList = new HashSet<Long>();
			for(int i=0; i< cl.length; i++)
			{
				if(cl[i].getName().matches("[0-9]+"))
				{
					idList.add(Long.parseLong(cl[i].getName()));
					quanity.put(Long.parseLong(cl[i].getName()), cl[i].getValue());  
				}
				
			}
			listsp = sanPhamService.getAllSanPhamByList(idList);
		}else     //Lay tu database
		{
			GioHang g = gioHangService.getGioHangByNguoiDung(currentUser);
			if (g == null) {g = new GioHang(); g.setNguoiDung(currentUser); gioHangService.save(g);}
			List<ChiMucGioHang> listchimuc = chiMucGioHangService.getChiMucGioHangByGioHang(g);
			ChiMucGioHang chiMucGioHang;
			int flag;
			var soLuong = 1;
			for(int i=0; i< cl.length; i++)
			{
				flag = 0;
				if(cl[i].getName().matches("[0-9]+"))
				{
					long id = Long.parseLong(cl[i].getName());
					soLuong = Integer.parseInt(cl[i].getValue());
					for (int j=0;j<listchimuc.size();j++)
					{
						chiMucGioHang = listchimuc.get(j);

						if (id == chiMucGioHang.getSanPham().getId())
						{
							chiMucGioHang.setSo_luong(chiMucGioHang.getSo_luong() + soLuong);

							flag = 1;
							break;
						}

						chiMucGioHangService.saveChiMucGiohang(chiMucGioHang);	
					}

					if (flag == 0)
					{
						chiMucGioHang = new ChiMucGioHang();
						chiMucGioHang.setGioHang(g);
						chiMucGioHang.setSanPham(sanPhamService.getSanPhamById(id));
						chiMucGioHang.setSo_luong(soLuong);
						listchimuc.add(chiMucGioHang);

						chiMucGioHangService.saveChiMucGiohang(chiMucGioHang);	
					}
				}
			}

			for(ChiMucGioHang c: listchimuc)
			{
				listsp.add(c.getSanPham());
				quanity.put(c.getSanPham().getId(), Integer.toString(c.getSo_luong()));				
			}

			ClearUpRightBeforeCheckout(req, res);
		}

		model.addAttribute("checkEmpty",listsp.size());
		model.addAttribute("cart",listsp);
		model.addAttribute("quanity",quanity);
		
		
		return "client/cart";
	}

	public void ClearUpRightBeforeCheckout(HttpServletRequest request, HttpServletResponse response)
	{
		Cookie clientCookies[] = request.getCookies();
		for(int i=0;i<clientCookies.length;i++)
		{
			if(clientCookies[i].getName().matches("[0-9]+"))
			{						
				clientCookies[i].setMaxAge(0);
				clientCookies[i].setPath("/laptopshop");
				response.addCookie(clientCookies[i]);
			}
		}
	}
}
