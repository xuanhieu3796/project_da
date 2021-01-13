package com.linkin.web.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.linkin.model.BillProductDTO;
import com.linkin.model.CategoryDTO;
import com.linkin.model.CommentDTO;
import com.linkin.model.KichThuocDTO;
import com.linkin.model.ProductDTO;
import com.linkin.model.ReviewDTO;
import com.linkin.model.ThuongHieuDTO;
import com.linkin.model.UserDTO;
import com.linkin.service.CategoryService;
import com.linkin.service.CommentService;
import com.linkin.service.KichThuocService;
import com.linkin.service.ProductService;
import com.linkin.service.ReviewService;
import com.linkin.service.ThuongHieuService;
import com.linkin.service.UserService;

@Controller
public class ClientController {

	@Autowired
	private CommentService commentService;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private UserService userService;

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private ThuongHieuService thuongHieuService;

	@Autowired
	private KichThuocService kichThuocService;

	@GetMapping(value = "/login")
	public String login(HttpServletRequest request, @RequestParam(name = "err", required = false) String error) {
		if (error != null) {
			request.setAttribute("err", error);
		}
		return "client/login";
	}

	@GetMapping(value = "/register")
	public String register(HttpServletRequest request, Model model) {
		model.addAttribute("userDTO", new UserDTO());
		return "client/register";
	}

	@PostMapping(value = "/register")
	public String register(@ModelAttribute(name = "userDTO") @Valid UserDTO userDTO, BindingResult bindingResult,
			HttpServletRequest request) {
		if (bindingResult.hasErrors()) {
			return "client/register";
		}
		userDTO.setEnabled(true);
		// userDTO.setRole(RoleEnum.MEMBER.getRoleName());

		try {
			userService.insert(userDTO);
		} catch (Exception exception) {
			request.setAttribute("msg", "Thông tin nhập đã tồn tại");
			return "client/register";
		}
		return "redirect:/login";
	}

	@GetMapping(value = "/products")
	public String findProducts(HttpServletRequest request, HttpSession session) {

		String thuongHieuName = request.getParameter("thuongHieuName") == null ? ""
				: request.getParameter("thuongHieuName");
		String categoryName = request.getParameter("categoryName") == null ? "" : request.getParameter("categoryName");

		String kichThuocName = request.getParameter("kichThuocName") == null ? ""
				: request.getParameter("kichThuocName");

		String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword");

		Long priceStart = (request.getParameter("priceStart") == null || request.getParameter("priceStart") == "") ? 1
				: Long.valueOf(request.getParameter("priceStart"));

		Integer page = request.getParameter("page") == null ? 1 : Integer.valueOf(request.getParameter("page"));

		Long priceEnd = (request.getParameter("priceEnd") == null || request.getParameter("priceEnd") == "")
				? 1000000000
				: Long.valueOf(request.getParameter("priceEnd"));

		List<ProductDTO> listPro = productService.search(keyword, categoryName, thuongHieuName, kichThuocName,
				priceStart, priceEnd, 0, page * 100);
		// mooi sp lay 1 sp vs 1 size show ra trang chu
		for (int i = 0; i < listPro.size() - 1; i++) {// tao vong lap theo bien i bat dau tu 0 dau cua listPro
			for (int j = listPro.size() - 1; j > i; j--) {// tao vong lap theo bien j bat dau tu phan tu cuoi listPro
				if (listPro.get(i).getName().equals(listPro.get(j).getName())) {// neu ten cua 2 sp thu i va thu j giong
																				// nhau thi xoa ptu thu j trong listPro
					listPro.remove(listPro.get(j));
				}
			}
		}

		List<CategoryDTO> list = categoryService.search("", 0, page * 100);
		List<ThuongHieuDTO> thuongHieuDTOs = thuongHieuService.search("", 0, page * 100);
		List<KichThuocDTO> kichThuocDTOs = kichThuocService.search("", 0, page * 100);

		List<CategoryDTO> list1 = categoryService.searchAll("");
		session.getAttribute("cate");
		session.setAttribute("cate", list1);

		request.setAttribute("kichThuocList", kichThuocDTOs);
		request.setAttribute("thuongHieuList", thuongHieuDTOs);
		request.setAttribute("productList", listPro);
		request.setAttribute("categoryList", list);

		request.setAttribute("kichThuocName", kichThuocDTOs);
		request.setAttribute("categoryName", categoryName);
		request.setAttribute("thuongHieuName", thuongHieuName);
		request.setAttribute("page", page);
		request.setAttribute("keyword", keyword);
		Object object = session.getAttribute("cart");
		if (object != null) {
			request.setAttribute("c", "as");
		}

		return "client/products";
	}

	@GetMapping(value = "/product")
	public String oneProduct(HttpServletRequest request, @RequestParam(name = "id", required = true) Long id,
			HttpSession session) {
		Integer page = request.getParameter("page") == null ? 1 : Integer.valueOf(request.getParameter("page"));

		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		ProductDTO product = productService.get(id);
		List<ProductDTO> list = productService.searchName(product.getName(), product.getCategory().getName(),
				product.getThuongHieuDTO().getName(), "", 1L, 100000000000L, 0, 10000000);// toan bo sp trong db cung
																							// ten vs sp o tren
		request.setAttribute("size", list);// day cac sp cung ten sang giao dien
		
		// sp lieen quan theo thuong hieu
		List<ProductDTO> listPro = productService.search("", "", product.getThuongHieuDTO().getName(), "", 1L,
				100000000L, 0, page * 100);
		for (int i = 0; i < listPro.size() - 1; i++) {// tao vong lap theo bien i bat dau tu 0 dau cua listPro
			for (int j = listPro.size() - 1; j > i; j--) {// tao vong lap theo bien j bat dau tu phan tu cuoi listPro
				if (listPro.get(i).getName().equals(listPro.get(j).getName())) {// neu ten cua 2 sp thu i va thu j giong
																				// nhau thi xoa ptu thu j trong listPro
					listPro.remove(listPro.get(j));
				}
			}
		}
		for (int i = 0; i < listPro.size(); i++) {/// bo sp dang xem chi tiet ra khoi listPro sp lien quan
			if(listPro.get(i).getName().equals(product.getName())) {
				listPro.remove(listPro.get(i));
			}
		}
		List<CommentDTO> commentDTOs = commentService.searchByProduct(id);// lay tat ca comment cua 1 sp 
		List<ReviewDTO> reviews = reviewService.find(id);// tuong tu comment
		float sum = 0;
		for (ReviewDTO reviewDTO : reviews) {
			int star = reviewDTO.getStarNumber();
			sum = sum + star;
		}
		int dem = reviews.size();
		float totalStar = sum / dem;// sao trung binh
		request.setAttribute("dem", dem);
		request.setAttribute("totalStar", totalStar);
		request.setAttribute("product", product);
		request.setAttribute("commentProduct", commentDTOs);
		request.setAttribute("reviews", reviews);
		request.setAttribute("productList", listPro);
		return "client/product";
	}

	@GetMapping(value = "/member/add-to-cart")
	public String AddToCart(@RequestParam(name = "pid") Long pId, HttpSession session, HttpServletRequest request)
			throws IOException {
		ProductDTO product = productService.get(pId);// lay thong tin sp tu trang products , product
		List<CategoryDTO> list = categoryService.searchAll("");
		request.setAttribute("categoryList", list);
		Object object = session.getAttribute("cart");// lay session neu co , neu chua co tao 1 session moi la cart
		if (object == null) {// neu cart rong 
			BillProductDTO billProduct = new BillProductDTO();
			billProduct.setProduct(product);// them sp vao cart
			billProduct.setQuantity(1);// sl sp =1
			billProduct.setUnitPrice(product.getPriceOut());// gia ban cua sp
			Map<Long, BillProductDTO> map = new HashMap<>();// luu tt sp vao map
			map.put(pId, billProduct);
			session.setAttribute("cart", map);// set cart bang map
		} else {// neu cart da co sp
			Map<Long, BillProductDTO> map = (Map<Long, BillProductDTO>) object;// lay ra map 
			BillProductDTO billProduct = map.get(pId);// tim danh sach sp co trong map
			if (billProduct == null) {// neu chua co sp trong map thi lay tt sp va sl sp =1
				billProduct = new BillProductDTO();
				billProduct.setProduct(product);
				billProduct.setQuantity(1);
				billProduct.setUnitPrice(product.getPriceOut());
				map.put(pId, billProduct);
			} else {// neu co sp trong map roi thi tang sl cua sp len +1
				if (billProduct.getQuantity() < product.getSoLuong()) {
					billProduct.setQuantity(billProduct.getQuantity() + 1);
				} else {
					billProduct.setQuantity(billProduct.getQuantity());
				}

			}
			session.setAttribute("cart", map);

		}

		return "redirect:/cart";
	}
	// tuong tu add-to-cart thi sub-to-cart la giam sl sp trong gio hang
	@GetMapping(value = "/member/sub-to-cart")
	public String subToCart(@RequestParam(name = "pid") Long pId, HttpSession session, HttpServletRequest request)
			throws IOException {
		ProductDTO product = productService.get(pId);
		Object object = session.getAttribute("cart");
		if (object == null) {
			BillProductDTO billProduct = new BillProductDTO();
			billProduct.setProduct(product);
			billProduct.setQuantity(1);
			billProduct.setUnitPrice(product.getPriceOut());
			Map<Long, BillProductDTO> map = new HashMap<>();
			map.put(pId, billProduct);
			session.setAttribute("cart", map);
		} else {
			Map<Long, BillProductDTO> map = (Map<Long, BillProductDTO>) object;
			BillProductDTO billProduct = map.get(pId);
			if (billProduct == null) {
				billProduct = new BillProductDTO();
				billProduct.setProduct(product);
				billProduct.setQuantity(1);
				billProduct.setUnitPrice(product.getPriceOut());
				map.put(pId, billProduct);
			} else {
				if (billProduct.getQuantity() >= 1) {
					billProduct.setQuantity(billProduct.getQuantity() - 1);
				} else {
					billProduct.setQuantity(billProduct.getQuantity());
				}

			}
			session.setAttribute("cart", map);

		}
		return "redirect:/cart";
	}
	// xoa tat ca sp co trong gio hang
	@GetMapping(value = "/delete-from-cart")
	public String Deletefromtocart(HttpServletRequest req, @RequestParam(name = "key", required = true) Long key,
			HttpSession session, HttpServletRequest request) {
		// session = req.getSession();
		Object object = session.getAttribute("cart");
		if (object != null) {
			Map<Long, BillProductDTO> map = (Map<Long, BillProductDTO>) object;
			map.remove(key);
			session.setAttribute("cart", map);
		}
		return "redirect:/cart";
	}
	// goi ra form gio hang
	@GetMapping(value = "/cart")
	public String cart(HttpServletRequest request, HttpSession session) {
		List<CategoryDTO> list = categoryService.searchAll("");
		request.setAttribute("categoryList", list);
		// lay tat ca sp co trong gio hang
		// tinh tong tien va dua ra giao dien
		Map<Long, BillProductDTO> map = (Map<Long, BillProductDTO>) session.getAttribute("cart");
		Long tongtien = 0L;
		for (Map.Entry<Long, BillProductDTO> entry : map.entrySet()) {
			Long tong = entry.getValue().getUnitPrice() * entry.getValue().getQuantity();
			tongtien = tongtien + tong;
		}
		request.setAttribute("tongtien", tongtien);
		return "client/cart";
	}

	@GetMapping(value = "/category/search")
	public String searchCategory(HttpServletRequest request,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", required = false) Integer page) {
		page = page == null ? 1 : page;
		keyword = keyword == null ? "" : keyword;
		// mac dinh 10 ban ghi 1 trang
		List<CategoryDTO> categoryList1 = categoryService.search(keyword, 0, page * 100);

		request.setAttribute("categoryList", categoryList1);
		request.setAttribute("page", page);
		request.setAttribute("keyword", keyword);

		return "client/categories";
	}

	@GetMapping(value = "/productsByCategory")
	public String AdminProductSearch(HttpServletRequest request,
			@RequestParam(name = "id", required = true) Long categoryId) {
		List<CategoryDTO> list = categoryService.searchAll("");
		request.setAttribute("categoryList", list);
		String thuongHieuName = request.getParameter("thuongHieuName") == null ? ""
				: request.getParameter("thuongHieuName");

		String gioiTinhName = request.getParameter("gioiTinhName") == null ? "" : request.getParameter("gioiTinhName");

		String kichThuocName = request.getParameter("kichThuocName") == null ? ""
				: request.getParameter("kichThuocName");
		String keyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword");
		Integer page = request.getParameter("page") == null ? 1 : Integer.valueOf(request.getParameter("page"));

		Long priceStart = (request.getParameter("priceStart") == null || request.getParameter("priceStart") == "") ? 1
				: Long.valueOf(request.getParameter("priceStart"));
		Long priceEnd = (request.getParameter("priceEnd") == null || request.getParameter("priceEnd") == "") ? 10000000
				: Long.valueOf(request.getParameter("priceEnd"));

		List<ProductDTO> listPro = productService.searchByCategory(keyword, thuongHieuName, kichThuocName, priceStart,
				priceEnd, categoryId, 0, page * 100);
		for (int i = 0; i < listPro.size() - 1; i++) {// tao vong lap theo bien i bat dau tu 0 dau cua listPro
			for (int j = listPro.size() - 1; j > i; j--) {// tao vong lap theo bien j bat dau tu phan tu cuoi listPro
				if (listPro.get(i).getName().equals(listPro.get(j).getName())) {// neu ten cua 2 sp thu i va thu j giong
																				// nhau thi xoa ptu thu j trong listPro
					listPro.remove(listPro.get(j));
				}
			}
		}

		List<ThuongHieuDTO> thuongHieuDTOs = thuongHieuService.search("", 0, page * 100);
		List<KichThuocDTO> kichThuocDTOs = kichThuocService.search("", 0, page * 100);

		request.setAttribute("kichThuocList", kichThuocDTOs);
		request.setAttribute("thuongHieuList", thuongHieuDTOs);
		request.setAttribute("productList", listPro);

		request.setAttribute("kichThuocName", kichThuocDTOs);
		request.setAttribute("thuongHieuName", thuongHieuName);
		request.setAttribute("page", page);
		request.setAttribute("keyword", keyword);
		request.setAttribute("categoryId", categoryId);
		return "client/products-by-category";
	}

	@GetMapping(value = "/contact")
	public String contact(HttpServletRequest request) {
		List<CategoryDTO> list = categoryService.searchAll("");
		request.setAttribute("categoryList", list);

		return "client/contact";
	}

	@GetMapping(value = "/about")
	public String about(HttpServletRequest request) {
		List<CategoryDTO> list = categoryService.searchAll("");
		request.setAttribute("categoryList", list);
		return "client/about";
	}
}