package com.electro.service.inventory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class SeleProductTest {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, 10);
        driver.manage().window().maximize();
    }

    /**
     * Test case 226-1: Kiểm tra tạo sản phẩm trùng tên
     * 
     * Mục tiêu:
     * - Kiểm tra UI hiển thị thông báo lỗi khi tạo sản phẩm trùng tên
     */
    @Test
    public void testCreateDuplicateProductName() {
        // 1. Truy cập trang đăng nhập admin
        driver.get("http://localhost:3000/admin");

        // 2. Đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/div/div/form/div[1]/div/input")));
        WebElement passwordInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/div[2]/div/div[1]/input"));
        WebElement loginButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/button"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123456");
        loginButton.click();

        // 3. Chọn menu sản phẩm
        WebElement productMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div[1]/div/div[6]/a[1]")));
        productMenu.click();

        // 4. Bấm nút thêm sản phẩm
        WebElement addProductButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[1]/div[2]/a")));
        addProductButton.click();

        // 5. Điền thông tin sản phẩm với tên trùng
        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[2]/div/div/input")));
        WebElement codeInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[3]/div/div/input"));
        WebElement slugInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[4]/div/div/input"));
        WebElement skuInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[14]/table/tbody/tr/td[3]/div/div/input")); // XPath cho input SKU
        WebElement saveButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[3]/button[2]"));

        nameInput.sendKeys("test");
        codeInput.sendKeys("TEST-001");
        slugInput.sendKeys("san-pham-test-001");
        skuInput.sendKeys("SKU-001");
        saveButton.click();

        // 6. Kiểm tra thông báo lỗi
        WebElement errorMessage = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[2]/div/div/div/div")));
        Assert.assertEquals(errorMessage.getText(), "Tên sản phẩm đã tồn tại", "Nội dung thông báo lỗi không khớp");
    }

    /**
     * Test case 226-2: Kiểm tra tạo sản phẩm trùng mã
     * 
     * Mục tiêu:
     * - Kiểm tra UI hiển thị thông báo lỗi khi tạo sản phẩm trùng mã
     */
    @Test
    public void testCreateDuplicateProductCode() {
        // 1. Truy cập trang đăng nhập admin
        driver.get("http://localhost:3000/admin");

        // 2. Đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/div/div/form/div[1]/div/input")));
        WebElement passwordInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/div[2]/div/div[1]/input"));
        WebElement loginButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/button"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123456");
        loginButton.click();

        // 3. Chọn menu sản phẩm
        WebElement productMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div[1]/div/div[6]/a[1]")));
        productMenu.click();

        // 4. Bấm nút thêm sản phẩm
        WebElement addProductButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[1]/div[2]/a")));
        addProductButton.click();

        // 5. Điền thông tin sản phẩm với mã trùng
        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[2]/div/div/input")));
        WebElement codeInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[3]/div/div/input"));
        WebElement slugInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[4]/div/div/input"));
        WebElement skuInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[14]/table/tbody/tr/td[3]/div/div/input")); // XPath cho input SKU
        WebElement saveButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[3]/button[2]"));

        nameInput.sendKeys("Sản phẩm test 002");
        codeInput.sendKeys("testset");
        slugInput.sendKeys("san-pham-test-002");
        skuInput.sendKeys("SKU-002");
        saveButton.click();

        // 6. Kiểm tra thông báo lỗi
        WebElement errorMessage = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[2]/div/div/div/div")));
        Assert.assertEquals(errorMessage.getText(), "Mã sản phẩm đã tồn tại", "Nội dung thông báo lỗi không khớp");
    }

    /**
     * Test case 226-3: Kiểm tra tạo sản phẩm trùng slug
     * 
     * Mục tiêu:
     * - Kiểm tra UI hiển thị thông báo lỗi khi tạo sản phẩm trùng slug
     */
    @Test
    public void testCreateDuplicateProductSlug() {
        // 1. Truy cập trang đăng nhập admin
        driver.get("http://localhost:3000/admin");

        // 2. Đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/div/div/form/div[1]/div/input")));
        WebElement passwordInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/div[2]/div/div[1]/input"));
        WebElement loginButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/button"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123456");
        loginButton.click();

        // 3. Chọn menu sản phẩm
        WebElement productMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div[1]/div/div[6]/a[1]")));
        productMenu.click();

        // 4. Bấm nút thêm sản phẩm
        WebElement addProductButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[1]/div[2]/a")));
        addProductButton.click();

        // 5. Điền thông tin sản phẩm với slug trùng
        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[2]/div/div/input")));
        WebElement codeInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[3]/div/div/input"));
        WebElement slugInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[4]/div/div/input"));
        WebElement skuInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[14]/table/tbody/tr/td[3]/div/div/input")); // XPath cho input SKU
        WebElement saveButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[3]/button[2]"));

        nameInput.sendKeys("Sản phẩm test 003");
        codeInput.sendKeys("TEST-003");
        slugInput.sendKeys("séttts");
        skuInput.sendKeys("SKU-003");
        saveButton.click();

        // 6. Kiểm tra thông báo lỗi
        WebElement errorMessage = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[2]/div/div/div/div")));
        Assert.assertEquals(errorMessage.getText(), "Slug sản phẩm đã tồn tại", "Nội dung thông báo lỗi không khớp");
    }

    /**
     * Test case 226-4: Kiểm tra tạo sản phẩm trùng SKU
     * 
     * Mục tiêu:
     * - Kiểm tra UI hiển thị thông báo lỗi khi tạo sản phẩm trùng SKU
     */
    @Test
    public void testCreateDuplicateProductSku() {
        // 1. Truy cập trang đăng nhập admin
        driver.get("http://localhost:3000/admin");

        // 2. Đăng nhập
        WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/div/div/form/div[1]/div/input")));
        WebElement passwordInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/div[2]/div/div[1]/input"));
        WebElement loginButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/div/div/form/button"));

        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("123456");
        loginButton.click();

        // 3. Chọn menu sản phẩm
        WebElement productMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/nav/div/div[1]/div/div[6]/a[1]")));
        productMenu.click();

        // 4. Bấm nút thêm sản phẩm
        WebElement addProductButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/div/div/main/div/div[1]/div[2]/a")));
        addProductButton.click();

        // 5. Điền thông tin sản phẩm với SKU trùng
        WebElement nameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[2]/div/div/input")));
        WebElement codeInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[3]/div/div/input"));
        WebElement slugInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[4]/div/div/input"));
        WebElement skuInput = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[1]/div[14]/table/tbody/tr/td[3]/div/div/input")); // XPath cho input SKU
        WebElement saveButton = driver.findElement(By.xpath("/html/body/div[1]/div/div/main/div/form/div/div/div[3]/button[2]"));

        nameInput.sendKeys("Sản phẩm test 004");
        codeInput.sendKeys("TEST-004");
        slugInput.sendKeys("san-pham-test-004");
        skuInput.sendKeys("s");
        saveButton.click();

        // 6. Kiểm tra thông báo lỗi
        WebElement errorMessage = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div[2]/div/div/div/div")));
        Assert.assertEquals(errorMessage.getText(), "SKU sản phẩm đã tồn tại", "Nội dung thông báo lỗi không khớp");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}