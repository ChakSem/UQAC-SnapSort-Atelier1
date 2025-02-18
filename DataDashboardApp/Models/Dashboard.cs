using DashboardApp.Db;
using System;
using System.Collections.Generic;
using System.Data.SqlClient;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace DashboardApp.Models
{
    public struct RevenueByDate
    {
        public string Date { get; set; }
        public decimal TotalAmount { get; set; }
    }

    public class Dashboard
    {
        //Fields & Properties
        private DateTime startDate;
        private DateTime endDate;
        private int numberDays;

        public int NumCustomers { get; private set; }
        public int NumSuppliers { get; private set; }
        public int NumProducts { get; private set; }
        public List<KeyValuePair<string, int>> TopProductsList { get; private set; }
        public List<KeyValuePair<string, int>> UnderstockList { get; private set; }
        public List<RevenueByDate> GrossRevenueList { get; private set; }
        public int NumOrders { get; set; }
        public decimal TotalRevenue { get; set; }
        public decimal TotalProfit { get; set; }

        //Constructor
        public Dashboard()
        {
            // Initializing with default values
            NumCustomers = 100;
            NumSuppliers = 50;
            NumProducts = 200;
            NumOrders = 300;
            TotalRevenue = 50000;
            TotalProfit = TotalRevenue * 0.2m; // 20% profit

            TopProductsList = new List<KeyValuePair<string, int>>()
        {
            new KeyValuePair<string, int>("Product 1", 150),
            new KeyValuePair<string, int>("Product 2", 120),
            new KeyValuePair<string, int>("Product 3", 100),
            new KeyValuePair<string, int>("Product 4", 90),
            new KeyValuePair<string, int>("Product 5", 80)
        };

            UnderstockList = new List<KeyValuePair<string, int>>()
        {
            new KeyValuePair<string, int>("Product A", 5),
            new KeyValuePair<string, int>("Product B", 3),
            new KeyValuePair<string, int>("Product C", 4)
        };

            GrossRevenueList = new List<RevenueByDate>()
        {
            new RevenueByDate { Date = "2024-02-01", TotalAmount = 2000 },
            new RevenueByDate { Date = "2024-02-02", TotalAmount = 2500 },
            new RevenueByDate { Date = "2024-02-03", TotalAmount = 3000 }
        };
        }

        //Private methods
        private void GetNumberItems()
        {
            // Here we can simulate the data
            NumCustomers = 100;
            NumSuppliers = 50;
            NumProducts = 200;
            NumOrders = 300;
        }

        private void GetProductAnalisys()
        {
            // Simulating the analysis
            TopProductsList = new List<KeyValuePair<string, int>>()
        {
            new KeyValuePair<string, int>("Product 1", 150),
            new KeyValuePair<string, int>("Product 2", 120),
            new KeyValuePair<string, int>("Product 3", 100),
            new KeyValuePair<string, int>("Product 4", 90),
            new KeyValuePair<string, int>("Product 5", 80)
        };

            UnderstockList = new List<KeyValuePair<string, int>>()
        {
            new KeyValuePair<string, int>("Product A", 5),
            new KeyValuePair<string, int>("Product B", 3),
            new KeyValuePair<string, int>("Product C", 4)
        };
        }

        private void GetOrderAnalisys()
        {
            // Simulating revenue data
            GrossRevenueList = new List<RevenueByDate>()
        {
            new RevenueByDate { Date = "2024-02-01", TotalAmount = 2000 },
            new RevenueByDate { Date = "2024-02-02", TotalAmount = 2500 },
            new RevenueByDate { Date = "2024-02-03", TotalAmount = 3000 }
        };

            TotalRevenue = GrossRevenueList.Sum(r => r.TotalAmount);
            TotalProfit = TotalRevenue * 0.2m; // 20% profit
        }

        //Public methods
        public bool LoadData(DateTime startDate, DateTime endDate)
        {
            endDate = new DateTime(endDate.Year, endDate.Month, endDate.Day,
                    endDate.Hour, endDate.Minute, 59);

            if (startDate != this.startDate || endDate != this.endDate)
            {
                this.startDate = startDate;
                this.endDate = endDate;
                this.numberDays = (endDate - startDate).Days;

                GetNumberItems();
                GetProductAnalisys();
                GetOrderAnalisys();
                Console.WriteLine("Refreshed data: {0} - {1}", startDate.ToString(), endDate.ToString());
                return true;
            }
            else
            {
                Console.WriteLine("Data not refreshed, same query: {0} - {1}", startDate.ToString(), endDate.ToString());
                return false;
            }
        }
    }
}



