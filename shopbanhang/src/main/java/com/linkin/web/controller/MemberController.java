package com.linkin.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.linkin.model.BillDTO;
import com.linkin.model.BillProductDTO;
import com.linkin.model.CategoryDTO;
import com.linkin.model.CommentDTO;
import com.linkin.model.InforBillDTO;
import com.linkin.model.ProductDTO;
import com.linkin.model.ReviewDTO;
import com.linkin.model.SMSDTO;
import com.linkin.model.UserDTO;
import com.linkin.model.UserPrincipal;
import com.linkin.service.BillProductService;
import com.linkin.service.BillService;
import com.linkin.service.CategoryService;
import com.linkin.service.CommentService;
import com.linkin.service.EmailService;
import com.linkin.service.InforBillService;
//import com.linkin.service.EmailService;
import com.linkin.service.ProductService;
import com.linkin.service.ReviewService;
import com.linkin.service.SMSService;
import com.linkin.service.UserService;

@Controller
public class MemberController {

	@Autowired
	InforBillService inforBillService;

	@Autowired
	CommentService commentService;

	@Autowired
	ProductService productService;

	@Autowired
	UserService userService;

	@Autowired
	BillProductService billProductService;

	@Autowired
	BillService billService;

	@Autowired
	ReviewService reviewService;

	@Autowired
	SMSService smsService;

	@Autowired
	EmailService emailService;

	@Autowired
	CategoryService categoryService;
	// lay tt cua sp , user, comment luu xuong db 
	@PostMapping(value = "/member/comment")
	public String comment(HttpServletRequest request, @RequestParam(name = "productId") Long productId,
			@RequestParam(name = "comment") String comment) {
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long userId = principal.getId();
		UserDTO userDTO = new UserDTO();
		userDTO.setName(principal.getName());
		userDTO.setId(userId);
		CommentDTO commentDTO = new CommentDTO();
		commentDTO.setUserDTO(userDTO);
		ProductDTO productDTO = new ProductDTO();
		productDTO.setId(productId);
		commentDTO.setProductDTO(productDTO);
		commentDTO.setComment(comment);
		commentService.insert(commentDTO);
		return "redirect:/product?id=" + commentDTO.getProductDTO().getId();
	}
	// tuong tu comment
	@PostMapping(value = "/member/review")
	public String review(HttpServletRequest request, @ModelAttribute ReviewDTO reviewDTO,
			@RequestParam(name = "productId") Long productId, @RequestParam(name = "starNumber") int starNumber) {
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		// Long userId = principal.getId();
		int check = 0;
		List<ReviewDTO> list = reviewService.find(productId);
		if (list.isEmpty()) {
			UserDTO userDTO = new UserDTO();
			userDTO.setName(principal.getName());
			userDTO.setId(principal.getId());
			reviewDTO.setUserDTO(userDTO);
			ProductDTO productDTO = new ProductDTO();
			productDTO.setId(productId);
			reviewDTO.setProductDTO(productDTO);
			reviewDTO.setStarNumber(starNumber);
			reviewService.add(reviewDTO);
		}
		for (ReviewDTO reviewDTO2 : list) {// tim trong cac danh gia cua sp neu user da dang gia roi thi k the danh gia 2 lan
			if (reviewDTO2.getUserDTO().getId() == principal.getId()) {
				check = 1;
				break;

			} else {
				check = 2;
			}
		}
		if (check == 2) {
			UserDTO userDTO = new UserDTO();
			userDTO.setName(principal.getName());
			userDTO.setId(principal.getId());
			reviewDTO.setUserDTO(userDTO);
			ProductDTO productDTO = new ProductDTO();
			productDTO.setId(productId);
			reviewDTO.setProductDTO(productDTO);
			reviewDTO.setStarNumber(starNumber);
			reviewService.add(reviewDTO);
		}

		return "redirect:/product?id=" + productId;
	}

	@GetMapping(value = "/member/bill/add")
	public String addOrder(HttpSession session, @RequestParam(value = "page", required = false) Integer page,
			Model model, HttpServletRequest request) throws IOException {
		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		// lay member dang nhap hien tai
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		UserDTO user = new UserDTO();
		user.setId(principal.getId());

		// lay sp trong gio hang
		Object object = session.getAttribute("cart");

		if (object != null) {
			Map<String, BillProductDTO> map = (Map<String, BillProductDTO>) object;
			// lay cac tt cua sp , user luu xuong db
			BillDTO bill = new BillDTO();
			bill.setUser(user);
			bill.setStatus("NEW");
			bill.setTrangThai("NEW");
			bill.setGiaoHang("NEW");
			billService.insert(bill);
			InforBillDTO inforBillDTO1 = (InforBillDTO) session.getAttribute("inforBill");
			InforBillDTO inforBillDTO = new InforBillDTO();
			inforBillDTO.setBillDTO(bill);
			inforBillDTO.setAddress(inforBillDTO1.getAddress());
			inforBillDTO.setPhoneNumber(inforBillDTO1.getPhoneNumber());
			inforBillDTO.setMethod(inforBillDTO1.getMethod());
			inforBillDTO.setNote(inforBillDTO1.getNote());
			inforBillService.insert(inforBillDTO);
			// tinh tong tien va tong tien thu thuc te neu khach dc khuyen mai
			long totalPrice = 0L;
			long finalTotalPrice = 0L;
			for (Entry<String, BillProductDTO> entry : map.entrySet()) {
				BillProductDTO billProduct = entry.getValue();
				billProduct.setBill(bill);

				billProductService.insert(billProduct);

				totalPrice = totalPrice + (billProduct.getQuantity() * billProduct.getUnitPrice());
				finalTotalPrice = totalPrice;
				bill.setTotal(totalPrice);
				/// discount
				page = page == null ? 1 : page;
				List<BillDTO> list = billService.searchByBuyerId(principal.getId(), 0, 1000 * 10); // search trong bang
																									// bill
				// update so luong sp sau khi mua hang thanh cong.
				ProductDTO productDTO = productService.get(entry.getValue().getProduct().getId());
				productDTO.setSoLuong(productDTO.getSoLuong() - billProduct.getQuantity());
				productService.update(productDTO);
				// if (list.isEmpty() == true ) {// lan dau mua
				if (list.size() == 1) { // lan dau mua
					finalTotalPrice = (totalPrice - (billProduct.getQuantity() * billProduct.getUnitPrice() * 0));
					bill.setPriceTotal(finalTotalPrice);
					bill.setDiscountPercent(5);
					// bill.setStatus("OLD");
					// bill.setTotal(totalPrice);
					// model.addAttribute("a", totalPrice);
					System.out.println("khuyen mai 5% cho lan thanh toan dau tien cua ban!!!!" + bill.getPriceTotal());
					// chưa cho hiển thị dc giá lúc chưa giảm.
				} else {

					bill.setPriceTotal(totalPrice);
					bill.setDiscountPercent(0);
					bill.setStatus("OLD");
				}

				System.out.println("...................................................................."
						+ inforBillDTO1.getMethod());
				if (inforBillDTO1.getMethod().equals("Thanh toán bằng thẻ")) {
					bill.setTrangThai("NEWS");
				}

				billService.update(bill);// udpate lai gia 
			}
			// goi sms service
			SMSDTO smsdto = new SMSDTO();
			smsdto.setCustomerPhone(String.valueOf((principal.getPhone())));
			smsdto.setContent("Bạn vừa đặt hàng thành công đơn hàng trên ShopTTH!!!");

			smsService.sendSMS(smsdto);

			String content = "";

			for (BillProductDTO i : map.values()) {
				System.out.println(
						"sản phẩm: " + i.getProduct().getName() + " Số tiền: " + i.getUnitPrice() * i.getQuantity());
				content += "<p>Sản phẩm: " + i.getProduct().getName() + "<p>Số tiền: "
						+ i.getUnitPrice() * i.getQuantity() + " (đ); \n";
			}

			// gửi email sau khi thanh toán
			emailService.sendSimpleMessage(principal.getEmail(), "Chi tiết đơn hàng",

					"<html>" + "<body>" + "<h3>Bạn vừa đặt hàng thành công đơn hàng trên shopTTH!!! \n</h3>" + content
							+ "<p>\n Tổng tiền: " + finalTotalPrice + " (đ)"

							+ "</body>" + "</html>");
//					"Bạn vừa thanh toán thành công đơn hàng trên ShopTHT !!! \n" + content
//							+ "\n Tổng tiền: " + finalTotalPrice + " (đ)");

			// xóa cart khi đã thanh toán
			session.removeAttribute("cart");

			return "redirect:/member/bills";
		}
		return "redirect:/products";
	}
	// danh sach tat ca hoa don cua 1 khach hang
	@GetMapping(value = "/member/bills")
	public String bills(HttpServletRequest request, @RequestParam(value = "keyword", required = false) Long keyword,
			@RequestParam(value = "page", required = false) Integer page, HttpSession session,
			@RequestParam(value = "trangthai", required = false) String trangthai) {
		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		// lay ra nguoi dang nhap hien tai
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		page = page == null ? 1 : page;
		keyword = principal.getId();
		trangthai = trangthai == null ? "NEW" : trangthai;
		List<BillDTO> listBill = billService.searchByBuyerId(keyword, 0, 1000 * 10); // search trong bang bill
		/*List<BillDTO> billDTOs = new ArrayList<BillDTO>();
		List<BillDTO> billDTOs1 = new ArrayList<BillDTO>();
		List<BillDTO> billDTOs2 = new ArrayList<BillDTO>();
		List<BillDTO> billDTOs3 = new ArrayList<BillDTO>();
		List<BillDTO> billDTOs4 = new ArrayList<BillDTO>();

		for (BillDTO billDTO : listBill) {
			if (billDTO.getTrangThai().equals("NEW")||billDTO.getTrangThai().equals("NEWS")) {// don hang moi mua chua co xac nhan tu admin
				billDTOs.add(billDTO);

			}
			if (billDTO.getTrangThai().equals("DA XAC NHAN")) {// don hang da dc admin xac nhan
				billDTOs1.add(billDTO);

			}
			if (billDTO.getTrangThai().equals("HUY")) {// don hang bi huy, huy van chuyen
				billDTOs2.add(billDTO);

			}
			if (billDTO.getGiaoHang().equals("DANG VAN CHUYEN")) {// don hang dang duoc van chuyen
				billDTOs3.add(billDTO);

			}
			if (billDTO.getGiaoHang().equals("DA NHAN HANG")) {// don hang ma khach hang da nhan duoc
				billDTOs4.add(billDTO);

			}

		}
		/*if (trangthai.equals("NEW")) {// don hang moi mua chua co xac nhan tu admin
			request.setAttribute("bills", billDTOs);// danh sach cac bill moi chua dc xac nhan cua 1 client
		}
		if (trangthai.equals("DA XAC NHAN")) {// don hang da dc admin xac nhan
			request.setAttribute("bills", billDTOs1);// danh sach cac bill da dc amin xac nhan cua 1 client
		}
		if (trangthai.equals("HUY")) {// don hang bi huy, huy van chuyen
			request.setAttribute("bills", billDTOs2);// danh sach cac bill bij huy cua 1 client
		}
		if (trangthai.equals("DANG VAN CHUYEN")) {
			request.setAttribute("bills", billDTOs3);// danh sach cac bill dc van chuyen
		}
		if (trangthai.equals("DA NHAN HANG")) {
			request.setAttribute("bills", billDTOs4);// danh sach cac bill da nhan dc hang cua 1client
		}

		// // danh sach bill cua 1 client
		request.setAttribute("bills", billDTOs);
		request.setAttribute("bills", billDTOs1);
		request.setAttribute("bills", billDTOs2);
		request.setAttribute("bills", billDTOs3);
		request.setAttribute("bills", billDTOs4);*/
		request.setAttribute("bills", listBill);
		request.setAttribute("page", page);
		request.setAttribute("keyword", keyword);
		return "member/bills";
	}

	long billId;// lÆ°u táº¡m thá»�i giÃ¡ trá»‹ id bill taitle
	// chi tiet cac sp dc mua trong 1 bill
	@GetMapping(value = "/member/bill")
	public String billDetail(HttpServletRequest request, @RequestParam(name = "billId", required = true) Long billId,
			@RequestParam(value = "keyword", required = false) Long keyword,
			@RequestParam(value = "page", required = false) Integer page, HttpSession session) {
		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		/*
		 * BillDTO billDTO = billService.get(billId); request.setAttribute("bill",
		 * billDTO);
		 */
		this.billId = billId;
		page = page == null ? 1 : page;
		keyword = billId;
		List<BillProductDTO> listBillProduct = billProductService.searchByBill(keyword, 0, 1000 * 10);// danh sach san
																										// pham trong 1
		// bill
		request.setAttribute("billProducts", listBillProduct);
		request.setAttribute("page", page);
		request.setAttribute("keyword", keyword);
		return "member/bill";
	}

	// xÃ³a item in list bill
	@GetMapping(value = "/member/delete/bills")
	public String deleteBillsProduct(@RequestParam(name = "billId", required = true) Long billId) {
		billService.delete(billId);
		return "redirect:/member/bills";
	}

	// xÃ³a item in list bill detail
	@GetMapping(value = "/member/delete/bill")
	public String deleteBillProduct(@RequestParam(name = "billId", required = true) Long billId) {
		System.out.println(billId);
		System.out.println();
		billProductService.delete(billId);
		return "redirect:/member/bill?billId=" + this.billId;
	}

	@GetMapping(value = "/member/thanhtoan")
	public String thanhToanGet(HttpServletRequest request, HttpSession session) {
		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		return "member/thanhtoan";
	}

	@PostMapping(value = "/member/thanhtoan")
	public String thanhToanPost(@ModelAttribute InforBillDTO inforBillDTO, HttpSession httpSession,
			HttpServletRequest req, HttpSession session, HttpServletRequest request) {
		List<CategoryDTO> categoryList = (List<CategoryDTO>) session.getAttribute("cate");
		request.setAttribute("categoryList", categoryList);
		httpSession = req.getSession();
		InforBillDTO inforBillDTO2 = (InforBillDTO) httpSession.getAttribute("inforBill");// lay tt khach nhap tu session
		httpSession.setAttribute("inforBill", inforBillDTO);// dua ra giao dien sau khi anh thanh toan
		Map<Long, BillProductDTO> map = (Map<Long, BillProductDTO>) session.getAttribute("cart");
		Long tongtien = 0L;
		// tinh tong tien va dua ra giao dien
		for (Map.Entry<Long, BillProductDTO> entry : map.entrySet()) {
			Long tong = entry.getValue().getUnitPrice() * entry.getValue().getQuantity();
			tongtien = tongtien + tong;
		}
		request.setAttribute("tongtien", tongtien);
		return "member/xacnhan";
	}
	// an da nhan hnag thi giao hang se tu DANG VAN CHUYEN doi sang DA NHAN HANG va luu xuong db
	@GetMapping(value = "/member/updateGiao/bill")
	public String updateGiaoHangBill(@RequestParam(name = "billId") Long billId, HttpSession httpSession,
			HttpServletRequest req) {
		httpSession = req.getSession();
		BillDTO billDTO = billService.get(billId);
		billDTO.setGiaoHang("DA NHAN HANG");
		billService.update(billDTO);
		return "redirect:/member/bills?trangthai=DANG+VAN+CHUYEN";
	}

}
