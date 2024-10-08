package marketMaster.service.restock;

import marketMaster.DTO.employee.EmployeeInfoDTO;
import marketMaster.DTO.product.ProductCategoryDTO;
import marketMaster.DTO.product.ProductIdDTO;
import marketMaster.DTO.product.ProductNameDTO;
import marketMaster.DTO.restock.RestockDTO;
import marketMaster.DTO.restock.RestockDetailViewDTO;
import marketMaster.bean.employee.EmpBean;
import marketMaster.bean.product.ProductBean;
import marketMaster.bean.restock.RestockBean;
import marketMaster.bean.restock.RestockDetailsBean;
import marketMaster.bean.restock.RestockDetailsId;
import marketMaster.service.employee.EmployeeRepository;
import marketMaster.service.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestockService {

    @Autowired
    private RestockRepository restockRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestockDetailRepository restockDetailRepository;

    /**
     * 取得今天最新的進貨 ID
     */

    public String getLatestRestockId() {
        // 取得當天的日期，格式為 YYYYMMDD
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // 使用 '%' + today + '%' 查詢所有今天的進貨 ID
        String restockIdPattern = "%" + today + "%";

        // 從資料庫中查詢最新的進貨 ID
        RestockBean latestRestock = restockRepository.findLatestRestockByDate(restockIdPattern).orElse(null);

        String newId;
        if (latestRestock != null) {
            // 如果找到最新的進貨 ID，解析出序號並增加 1
            String lastestId = latestRestock.getRestockId();
            int sequence = Integer.parseInt(lastestId.substring(8)) + 1;  // 提取 ID 中的序號部分
            newId = String.format("%s%03d", today, sequence);  // 格式化新的 ID，序號保持三位
        } else {
            // 如果今天還沒有進貨 ID，則從 "001" 開始
            newId = today + "001";
        }

        return newId;
    }

    /**
     * 取得所有員工資訊
     */
    public List<EmployeeInfoDTO> getEmployeeInfo() {
        return employeeRepository.findAllEmployeeInfo();
    }

    /**
     * 取得所有產品類別
     */
    public List<ProductCategoryDTO> getProductCategory() {
        return productRepository.findAllCategories();
    }

    /**
     * 根據產品類別取得所有產品名稱
     */
    public List<ProductNameDTO> getAllProductNamesByCategory(String productCategory) {
        return productRepository.findAllProductNamesByCategory(productCategory);
    }

    public List<ProductIdDTO> findAllProductIdByProductName(String productName) {
        return productRepository.findAllProductIdByProductName(productName);
    }

    /**
     * 插入進貨資料
     */
    @Transactional
    public void insertRestockData(RestockDTO restockDTO) {
        // 根據 employeeId 獲取 EmpBean 對象
        EmpBean employee = employeeRepository.findById(restockDTO.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Employee ID: " + restockDTO.getEmployeeId()));

        // 創建 RestockBean 對象
        RestockBean restock = new RestockBean();
        restock.setRestockId(restockDTO.getRestockId());
        restock.setEmployee(employee);
        restock.setRestockTotalPrice(restockDTO.getRestockTotalPrice());
        restock.setRestockDate(restockDTO.getRestockDate());

        // 處理 RestockDetailsBean
        List<RestockDetailsBean> detailsList = restockDTO.getRestockDetails().stream().map(dto -> {
            RestockDetailsId id = new RestockDetailsId(restockDTO.getRestockId(), dto.getProductId());
            RestockDetailsBean details = new RestockDetailsBean();
            details.setId(id);

            // 從 productRepository 獲取 ProductBean 並設置
            ProductBean product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Product ID: " + dto.getProductId()));
            details.setProduct(product);

            // 設置進貨詳細資訊
            details.setRestock(restock);
            details.setNumberOfRestock(dto.getNumberOfRestock());
            details.setProductName(dto.getProductName());
            details.setProductPrice(dto.getProductPrice());
            details.setRestockTotalPrice(dto.getRestockTotalPrice());
            details.setProductionDate(dto.getProductionDate());
            details.setDueDate(dto.getDueDate());
            details.setRestockDate(dto.getRestockDate());

            return details;
        }).collect(Collectors.toList());

        // 設置 RestockDetails
        restock.setRestockDetails(detailsList);

        // 保存 RestockBean（同時保存 RestockDetailsBean）
        restockRepository.save(restock);
    }

    public List<RestockDetailViewDTO> getAllRestockDetail() {
        return restockDetailRepository.getAllRestockDetails();

    }

    /*
    刪除資料
     */
    public void delete(String restockId) {
        restockRepository.deleteById(restockId);
        System.out.println("Restock deleted: " + restockId);
    }


    public List<RestockDetailViewDTO> findRestockDetailsByDateRange(String startDate, String endDate) {
        return restockDetailRepository.findRestockDetailsByDateRange(startDate, endDate);
    }

    public void updateRestockDetail(RestockDetailViewDTO restockDetailViewDTO) {
        restockDetailRepository.updateRestockDetail(
                restockDetailViewDTO.getRestockId(),
                restockDetailViewDTO.getProductId(),
                restockDetailViewDTO.getNumberOfRestock(),
                restockDetailViewDTO.getProductPrice(),
                restockDetailViewDTO.getProductionDate(),
                restockDetailViewDTO.getDueDate()
        );

    }
}
