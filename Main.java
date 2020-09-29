import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;
import javax.management.loading.PrivateClassLoader;
import javax.swing.event.CaretListener;
import java.util.List;
import java.util.ArrayList;

class Main {
    public static void main(final String[] args) {
        List<ParkingInstance> parkingInstanceList = ingestFile(args[0]);
        List<Car> cars = createCars(parkingInstanceList);
        sortList(parkingInstanceList, cars);
        findPrePay(parkingInstanceList, cars);
        BigDecimal totalCharged = new BigDecimal("0.00");
        totalCharged = chargePerCar(cars, totalCharged);
        System.out.println(" ");
        System.out.println("TOTAL CARS PARKED: " + cars.size());
        System.out.println("TOTAL CHARGED: $" + totalCharged);
    }

    private static BigDecimal chargePerCar(List<Car> cars, BigDecimal totalCharge) {
        for (Car car : cars) {
            for (int i = 0; i < car.getParkingInstances().size() - 1; i = i + 2) {
                if (car.getParkingInstances().get(i).getParkingPass().equals("Y") && car.getParkingInstances().get(i + 1).getParkingPass().equals("Y")) {
                    totalCharge = displayMessage(car.getLicensePlate(), car.getParkingInstances().get(i + 1).getTime(), new BigDecimal("0.00"), totalCharge);
                } else {
                    int minutes = minutesParked(car.getParkingInstances().get(i).getTime(), car.getParkingInstances().get(i + 1).getTime());
                    BigDecimal price = determinePrice(minutes);

                    int compareInOutDiscount = car.getParkingInstances().get(i).getDiscount().compareTo(car.getParkingInstances().get(i + 1).getDiscount());
                    int compareDiscountToZero = car.getParkingInstances().get(i).getDiscount().compareTo(new BigDecimal("0.00"));

                    if ((compareInOutDiscount == 0) && compareDiscountToZero != 0) {
                        BigDecimal rounded = getDiscountedPrice(price, car.getParkingInstances().get(i).getDiscount()).setScale(2, RoundingMode.CEILING);
                        price = applyPrePay(car, rounded);
                        totalCharge = displayMessage(car.getLicensePlate(), car.getParkingInstances().get(i + 1).getTime(), price, totalCharge);
                        car.setTotalCash(car.getTotalCash().add(price));
                    } else {
                        price = applyPrePay(car, price);
                        totalCharge = displayMessage(car.getLicensePlate(), car.getParkingInstances().get(i + 1).getTime(), price, totalCharge);
                        car.setTotalCash(car.getTotalCash().add(price));
                    }
                }
            }
            printPerCar(car.getLicensePlate(), car.getTotalCash());
        }
        return totalCharge;
    }

    private static void printPerCar(String licPlate, BigDecimal totalCash) {
        System.out.println(" ");
        System.out.println("TOTAL FOR LICENSE PLATE " + licPlate + ": $" + totalCash);
        System.out.println(" ");
        System.out.println("-----------------------------");
    }

    private static BigDecimal applyPrePay(Car car, BigDecimal price) {
        BigDecimal zero = new BigDecimal("0.00");
        int compareZero = car.getPrePayAmount().compareTo(zero);
        if (compareZero == 1) {
            int comparePrice = car.getPrePayAmount().compareTo(price);
            if (comparePrice == 1) {
                car.setPrePayAmount(car.getPrePayAmount().subtract(price));
                price = new BigDecimal("0.00");
            } else {
                price = price.subtract(car.getPrePayAmount());
                car.setPrePayAmount(new BigDecimal("0.00"));
            }
        }
        return price;
    }

    private static List<Car> createCars(List<ParkingInstance> piList) {
        List<Car> cars = new ArrayList<>();
        for (ParkingInstance pil : piList) {
            if (cars.size() == 0) {
                cars.add(new Car(pil.getLicensePlate(), new BigDecimal("0.00")));
            }
            String add = "Y";
            for (Car c : cars) {
                if (pil.getLicensePlate().equals(c.getLicensePlate())) {
                    add = "N";
                }
            }
            if (add.equals("Y")) {
                cars.add(new Car(pil.getLicensePlate(), new BigDecimal("0.00")));
            }
        }
        return cars;
    }

    private static void findPrePay(List<ParkingInstance> piList, List<Car> cars) {
        for (ParkingInstance PI : piList) {
            BigDecimal zero = new BigDecimal("0.00");
            if (PI.getPrePaid() !=  zero) {
                for (Car car : cars) {
                    if (PI.getLicensePlate().equals(car.getLicensePlate())) {
                        car.setPrePayAmount(PI.getPrePaid());
                    }
                }
            }
        }
    }

    private static BigDecimal displayMessage(String plate, String depTime, BigDecimal charge, BigDecimal total) {
        System.out.println("License Plate: " + plate);
        System.out.println("Departure Time: " + depTime);
        System.out.println("Charge: $" + charge);
        System.out.println("-----------------------------");

        total = total.add(charge);
        return total;
    }

    private static List<ParkingInstance> ingestFile(String file) {
        List<ParkingInstance> PIarray = new ArrayList<>();
        try {
            Scanner sc = new Scanner(new File(file));
            sc.useDelimiter(",|\\n");
            int i = 1;
            while(sc.hasNext()) {
                if (i > 6) {
                    String licensePlate = sc.next();
                    String parkingPass = sc.next();
                    String prePaidString = sc.next();
                    String discountString = sc.next();
                    String direction = sc.next();
                    String time = sc.next();
                    
                    PIarray.add(createInstance(licensePlate, parkingPass, prePaidString, discountString, direction, time));
                } else {
                    sc.next();
                    i++;
                }
            }
            sc.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return PIarray;
    }

    private static void sortList(List<ParkingInstance> piList, List<Car> cars) {
        List<ParkingInstance> inList = makeInOutList(piList, "I");
        List<ParkingInstance> outList = makeInOutList(piList, "O");

        for (Car car : cars) {
            for (int i = 0; i < outList.size(); i++) {
                if (car.getLicensePlate().equals(inList.get(i).getLicensePlate()) && car.getLicensePlate().equals(outList.get(i).getLicensePlate())) {
                    car.addParkingInstance(inList.get(i));
                    car.addParkingInstance(outList.get(i));
                }
            }
        }
    }

    private static List<ParkingInstance> makeInOutList(List<ParkingInstance> piList, String direction) {
        List<ParkingInstance> retList = new ArrayList<>();
        for (ParkingInstance pi : piList) {
            if (pi.getDirection().equals(direction)) {
                retList.add(pi);
            }
        }
        return retList;
    }

    private static BigDecimal getDiscountedPrice(BigDecimal price, BigDecimal discount) {
        BigDecimal discountDecimal = discount.divide(new BigDecimal("100")); 
        BigDecimal mult = price.multiply(discountDecimal); 
        BigDecimal discountedPrice = price.subtract(mult);
        return discountedPrice;
    }

    private static ParkingInstance createInstance(String licPlate, String parkPass, String prePaid, String discount, String direction, String time) {
        BigDecimal prePaidBD = new BigDecimal("0.00");
        BigDecimal discountBD = new BigDecimal("0.00");

        if (!prePaid.isEmpty()) {
            prePaidBD = new BigDecimal(prePaid);
        }
        if (!discount.isEmpty()) {
            discountBD = new BigDecimal(discount);
        }

        ParkingInstance pi = new ParkingInstance(licPlate, parkPass, prePaidBD, discountBD, direction, time);

        return pi;
    }

    private static int minutesParked(String dateTimeIn, String dateTimeOut) {
        int time = 0;
        String[] split1 = dateTimeIn.split("T");
        String[] split2 = dateTimeOut.split("T");
        String inSplit = split1[0] + " " + split1[1];
        String outSplit = split2[0] + " " + split2[1];

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date d1 = null;
        Date d2 = null;

        try {
            d1 = format.parse(inSplit);
            d2 = format.parse(outSplit);

            long diff = d2.getTime() - d1.getTime();

            long min = diff / 60000;

            time = (int)min;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    private static BigDecimal determinePrice(int minsParked) {
        BigDecimal price = new BigDecimal("0.00");
        while (minsParked >= 1440) {
            price = price.add(new BigDecimal("20.00"));
            minsParked = minsParked - 1440;
        }
        if ((minsParked >= 360) && (minsParked <= 1439)) { //6 - 23:59
            price = price.add(new BigDecimal("16.00"));
        } else if ((minsParked >= 240) && (minsParked <= 359)) { //4 - 5:59
            price = price.add(new BigDecimal("15.00"));
        } else if ((minsParked >= 180) && (minsParked <= 239)) { //3 - 3:59
            price = price.add(new BigDecimal("12.00"));
        } else if ((minsParked >= 120) && (minsParked <= 179)) { //2 - 2:59
            price = price.add(new BigDecimal("9.00"));
        } else if ((minsParked >= 60) && (minsParked <= 119)) { //1 - 1:59
            price = price.add(new BigDecimal("6.00"));
        } else if ((minsParked >= 30) && (minsParked <= 59)) { //30 - 59
            price = price.add(new BigDecimal("3.00"));
        } else { //0 - 29
            price = price.add(new BigDecimal("1.00"));
        }
        return price;
    }
}

public class ParkingInstance {
    private String licensePlate;
    private String parkingPass;
    private BigDecimal prePaid;
    private BigDecimal discount;
    private String direction;
    private String dateTime;

    public ParkingInstance(String liPlate, String parkPass, BigDecimal prePay, BigDecimal dis, String dire, String dT) {
        licensePlate = liPlate;
        parkingPass = parkPass;
        prePaid = prePay;
        discount = dis;
        direction = dire;
        dateTime = dT;
    }

    public String getLicensePlate() {
        return this.licensePlate;
    }
    public void setLicensePlate(String lp) {
        this.licensePlate = lp;
    }

    public String getParkingPass() {
        return this.parkingPass;
    }
    public void setParkingPass(String pp) {
        this.parkingPass = pp;
    }

    public BigDecimal getPrePaid() {
        return this.prePaid;
    }
    public void setPrePaid(BigDecimal prep) {
        this.prePaid = prep;
    }

    public BigDecimal getDiscount() {
        return this.discount;
    }
    public void setDiscount(BigDecimal disc) {
        this.discount = disc;
    }

    public String getDirection() {
        return this.direction;
    }
    public void setDirection(String dir) {
        this.direction = dir;
    }

    public String getTime() {
        return this.dateTime;
    }
    public void setTime(String time) {
        this.dateTime = time;
    }
}

public class Car {
    private String licensePlate;
    private BigDecimal totalCash;
    private BigDecimal prePayAmount;
    private List<ParkingInstance> parkingInstances = new ArrayList<ParkingInstance>();

    public Car(String licPlate, BigDecimal amount) {
        licensePlate = licPlate;
        totalCash = amount;
    }

    public String getLicensePlate() {
        return this.licensePlate;
    }
    public void setLicensePlate(String lp) {
        this.licensePlate = lp;
    }

    public BigDecimal getTotalCash() {
        return this.totalCash;
    }
    public void setTotalCash(BigDecimal cash) {
        this.totalCash = cash;
    }

    public BigDecimal getPrePayAmount() {
        return this.prePayAmount;
    }
    public void setPrePayAmount(BigDecimal prePay) {
        this.prePayAmount = prePay;
    }

    public List<ParkingInstance> getParkingInstances() {
        return this.parkingInstances;
    }
    public void setParkingInstances(List<ParkingInstance> visits) {
        this.parkingInstances = visits;
    }
    public void addParkingInstance(ParkingInstance pi) {
        parkingInstances.add(pi);
    }
}