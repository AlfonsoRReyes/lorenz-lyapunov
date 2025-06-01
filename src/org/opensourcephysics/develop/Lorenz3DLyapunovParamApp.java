/*
 * Two Lorenz systems to be used to calculate the Lyapunov Exponent
 * Using Open Source Physics (OSP) Library
 * Allows for state variables and parameter perturbation 
 * and sensitivity analysis.
 */
package org.opensourcephysics.develop;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.frames.*;
import org.opensourcephysics.display3d.simple3d.*;
import org.opensourcephysics.numerics.*;
import org.opensourcephysics.display.*;
import java.awt.*;

/**
 * Lorenz3DLyapunovParamApp demonstrates the calculation of Lyapunov exponent
 * for the Lorenz system using state variables or parameter perturbation
 */
public class Lorenz3DLyapunovParamApp extends AbstractSimulation {
    
    // 3D Display for Lorenz attractor (x, y, z)
    Display3DFrame lorenzFrame = new Display3DFrame("Lorenz Attractor");
    LorenzLyapunovParam lorenz = new LorenzLyapunovParam();
    
    // Strip chart for Lyapunov exponent evolution (linear scale)
    PlotFrame lyapunovFrame = new PlotFrame("Time", "Lyapunov Exponent", "Lyapunov Exponent Evolution");
    Dataset lyapunovDataset = new Dataset(Color.RED);
    
    // Strip chart for Lyapunov exponent evolution (log scale)
    PlotFrame lyapunovLogFrame = new PlotFrame("Time", "Log(Lyapunov Exponent)", "Lyapunov Exponent Evolution (Log Scale)");
    Dataset lyapunovLogDataset = new Dataset(Color.MAGENTA);
    
    // Strip chart for 100-second Lyapunov sampling
    PlotFrame lyapunovSampleFrame = new PlotFrame("Time", "Lyapunov Exponent", "Lyapunov Convergence (100s Sampling)");
    Dataset lyapunovSampleDataset = new Dataset(Color.BLUE);
    
    // Strip chart for state variables evolution
    PlotFrame stateFrame = new PlotFrame("Time", "State Variables x,y,z", "Lorenz State Variables vs Time");
    Dataset xDataset = new Dataset();
    Dataset yDataset = new Dataset();
    Dataset zDataset = new Dataset();

    private double lastSampleTime = -100;
    
    // Simulation parameters
    private double timeWindow = 100.0; // Time window for strip chart
    
    /**
     * Constructs the Lorenz3DLyapunovParamApp application
     */
    public Lorenz3DLyapunovParamApp() {
        // Setup 3D Lorenz visualization
        lorenzFrame.setPreferredMinMax(-20.0, 20.0, -30.0, 30.0, 0.0, 50.0);
        lorenzFrame.setDecorationType(org.opensourcephysics.display3d.core.VisualizationHints.DECORATION_AXES);
        lorenzFrame.addElement(lorenz);
        
        // Setup linear Lyapunov strip chart
        lyapunovDataset.setConnected(true);
        lyapunovDataset.setLineColor(Color.RED);
        lyapunovDataset.setMarkerSize(-1);     // size of the LE curve
        lyapunovDataset.setMaximumPoints(1000000);
        lyapunovFrame.addDrawable(lyapunovDataset);
        lyapunovFrame.setAutoscaleX(true);   // Enable autoscale for X
        lyapunovFrame.setAutoscaleY(true);   // Enable autoscale for Y
        // Set Y-axis to start from 0, but keep Y-max autoscaling
        lyapunovFrame.setPreferredMinMaxY(0, Double.NaN);  // Y-min=0, Y-max=auto
        
        // Setup log scale Lyapunov strip chart (use built-in log scale)
        lyapunovLogDataset.setConnected(true);
        lyapunovLogDataset.setLineColor(Color.MAGENTA);
        lyapunovLogDataset.setMarkerSize(-1);
        lyapunovLogDataset.setMaximumPoints(10000000);
        lyapunovLogFrame.addDrawable(lyapunovLogDataset);
        lyapunovLogFrame.setAutoscaleX(true);  // Enable autoscale for X
        lyapunovLogFrame.setAutoscaleY(true);  // Enable autoscale for Y
        lyapunovLogFrame.setLogScale(false, true); // Linear time, log Lyapunov exponent
        
        // Setup 100-second sampling plot
        lyapunovSampleDataset.setConnected(true);
        lyapunovSampleDataset.setLineColor(Color.BLUE);
        lyapunovSampleDataset.setMarkerSize(3);  // Show markers for sparse data
        lyapunovSampleDataset.setMarkerColor(Color.BLUE);
        lyapunovSampleDataset.setMaximumPoints(100000);  // Plenty for long runs
        lyapunovSampleFrame.addDrawable(lyapunovSampleDataset);
        lyapunovSampleFrame.setAutoscaleX(true);
        lyapunovSampleFrame.setAutoscaleY(true);
        // lyapunovSampleFrame.setPreferredMinMaxY(0, Double.NaN);  // Y-min=0, Y-max=auto
        lyapunovSampleFrame.setPreferredMinMaxY(0, 1);  // Y-min=0, Y-max=1
        
        // Setup state variables strip chart - clean lines with better visibility
        xDataset.setConnected(true);
        xDataset.setMarkerSize(-1);  // Disable markers completely
        xDataset.setLineColor(Color.DARK_GRAY);  // Keep x variable opaque and visible
        stateFrame.addDrawable(xDataset);
        
        yDataset.setConnected(true);
        yDataset.setMarkerSize(-1);  // Disable markers completely
        yDataset.setLineColor(new Color(255, 165, 0, 120));  // Semi-transparent orange (alpha=120)
        stateFrame.addDrawable(yDataset);
        
        zDataset.setConnected(true);
        zDataset.setMarkerSize(-1);  // Disable markers completely
        zDataset.setLineColor(new Color(0, 255, 0, 120));    // Semi-transparent green (alpha=120)
        stateFrame.addDrawable(zDataset);
        
        stateFrame.setAutoscaleX(false);
        stateFrame.setAutoscaleY(true);
        stateFrame.setPreferredMinMaxX(0, timeWindow);
        
        // Position frames
        lorenzFrame.setLocation(50, 50);
        lyapunovFrame.setLocation(500, 50);
        lyapunovFrame.setSize(400, 300);
        lyapunovLogFrame.setLocation(950, 50);  // Position log plot to the right
        lyapunovLogFrame.setSize(400, 300);
        lyapunovSampleFrame.setLocation(1400, 50); // Position sampling plot to the far right
        lyapunovSampleFrame.setSize(400, 300);
        stateFrame.setLocation(500, 400);       // Move state variables below
        stateFrame.setSize(400, 300);
    }

    /**
     * Initializes the simulation
     */
    public void initialize() {
        // Read initial conditions for both Lorenz systems
        double x1 = control.getDouble("x1");
        double y1 = control.getDouble("y1");
        double z1 = control.getDouble("z1");
        double x2 = control.getDouble("x2");
        double y2 = control.getDouble("y2");
        double z2 = control.getDouble("z2");
        
        // Read parameters for system 1
        double sigma1 = control.getDouble("sigma1");
        double rho1 = control.getDouble("rho1");
        double beta1 = control.getDouble("beta1");
        
        // Read parameters for system 2
        double sigma2 = control.getDouble("sigma2");
        double rho2 = control.getDouble("rho2");
        double beta2 = control.getDouble("beta2");
        
        double dt = control.getDouble("dt");
        timeWindow = control.getDouble("time window");
        
        // Initialize Lorenz system with separate parameters
        lorenz.initialize(x1, y1, z1, x2, y2, z2, 
                         sigma1, rho1, beta1, sigma2, rho2, beta2);
        lorenz.ode_solver.initialize(dt);
        
        // Clear and setup strip charts with current time window
        lyapunovDataset.clear();
        lyapunovLogDataset.clear();
        lyapunovSampleDataset.clear();
        xDataset.clear();
        yDataset.clear();
        zDataset.clear();
        // Don't set PreferredMinMaxX for autoscaling plots
        stateFrame.setPreferredMinMaxX(0, timeWindow);
        lyapunovFrame.repaint();
        lyapunovLogFrame.repaint();
        lyapunovSampleFrame.repaint();
        stateFrame.repaint();
        
        // Make plot frames visible
        lorenzFrame.setVisible(true);
        lyapunovFrame.setVisible(true);
        lyapunovLogFrame.setVisible(true);
        lyapunovSampleFrame.setVisible(true);
        stateFrame.setVisible(true);
    }

    /**
     * Resets the simulation with default parameters
     */
    public void reset() {
        // Initial conditions for both systems (identical)
        control.setValue("x1", 1.0);
        control.setValue("y1", 0.0);
        control.setValue("z1", 0.0);
        control.setValue("x2", 1.000000001);  // 1 + 1e-9
        control.setValue("y2", 0.0);
        control.setValue("z2", 0.0);
        
        // Parameters for system 1 (reference)
        control.setValue("sigma1", 10.0);
        control.setValue("rho1", 28.0);
        control.setValue("beta1", 8.0/3.0);
        
        // Parameters for system 2 (perturbed sigma)
        control.setValue("sigma2", 10.0);  // can also do small perturbation in sigma
        control.setValue("rho2", 28.0);
        control.setValue("beta2", 8.0/3.0);
        
        // Integration parameters
        control.setValue("dt", 0.01);
        control.setValue("time window", 200.0);
        enableStepsPerDisplay(true);
    }

    /**
     * Performs one step of the simulation
     */
    protected void doStep() {
        // Step the Lorenz system multiple times for smoother visualization
        // for (int i = 0; i < 5; i++) {
        for (int i = 0; i < stepsPerDisplay; i++) {
            lorenz.doStep();
        }
        
        // Update displays (format without scientific notation)
        String timeStr = String.format("%.1f", lorenz.getTime());
        String lyapunovStr = String.format("%.4f", lorenz.getCurrentLyapunov());
        
        lyapunovFrame.setMessage("t=" + timeStr + ", λ=" + lyapunovStr);
        lyapunovLogFrame.setMessage("t=" + timeStr + ", λ=" + lyapunovStr);
        lyapunovSampleFrame.setMessage("t=" + timeStr + ", λ=" + lyapunovStr + " (100s samples)");
        lorenzFrame.setMessage("t=" + timeStr + ", λ=" + lyapunovStr);
        
        // Add point to Lyapunov strip chart with scrolling window
        double time = lorenz.getTime();
        double lyapunov = lorenz.getCurrentLyapunov();
        
        // Get current state variables
        double[] state = lorenz.getState();
        double x = state[0];
        double y = state[1];
        double z = state[2];
        
        // Debug output every 10 seconds to see timing
        if (time % 10 < 0.1) {
            System.out.printf("DEBUG: t=%.2f, time%%100=%.3f, condition=%b%n", 
                              time, time % 100, (time % 100 < 0.1));
        }
        
        // Plot the actual Lyapunov exponent and state variables
        if (time > 0.1) {
            // Add to both Lyapunov plots simultaneously (same data)
            lyapunovDataset.append(time, lyapunov);
            
            // Add to log scale plot (same raw data - OSP will handle log conversion)
            if (lyapunov > 0) {  // Only positive values can be plotted on log scale
                lyapunovLogDataset.append(time, lyapunov);  // Raw value, not log-converted
            }

            // Add to 100-second sampling plot
            // if (Math.abs(time % 100) < 0.05) {  // Sample every 100 time units
            // if (((int)(time + 0.01)) % 100 == 0) {  // Rounds to nearest integer
            // if (((int)(time + 0.1)) % 100 == 0) {  // Rounds to nearest integer
            // if (time - lastSampleTime >= 100.0) {  // Every 100 seconds
            if (Math.abs(time % 25) < 0.05 || time < 1.0) {
                lyapunovSampleDataset.append(time, lyapunov);
                System.out.printf("SAMPLE: t=%.1f LE=%.6f dataPoints=%d%n", time, lyapunov, lyapunovDataset.getIndex());
            }

            // Add to state variables plot
            xDataset.append(time, x);
            yDataset.append(time, y);
            zDataset.append(time, z);
            
            // Implement scrolling window for state variables only (both LE plots autoscale)
            if (time > timeWindow) {
                // Calculate new window bounds
                double windowStart = time - timeWindow;
                double windowEnd = time;
                
                // Update only state variables plot window (LE plots autoscale)
                stateFrame.setPreferredMinMaxX(windowStart, windowEnd);
            }
        }
        
        lyapunovFrame.repaint();
        lyapunovLogFrame.repaint();
        lyapunovSampleFrame.repaint();
        stateFrame.repaint();
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        SimulationControl.createApp(new Lorenz3DLyapunovParamApp());
    }
}

/**
 * LorenzLyapunov combines 3D visualization with Lyapunov calculation
 * using parameter perturbation method
 */
class LorenzLyapunovParam extends Group implements ODE {
    // State arrays: [x1, y1, z1, x2, y2, z2, t]
    double[] state = new double[7];
    
    // Lorenz parameters for system 1
    double sigma1 = 10.0;
    double rho1 = 28.0;
    double beta1 = 8.0/3.0;
    
    // Lorenz parameters for system 2 (perturbed)
    double sigma2 = 10.0;
    double rho2 = 28.0;
    double beta2 = 8.0/3.0;
    
    // Lyapunov calculation
    double initialSeparation = 0.0;   // this was used in the previous version
    double lyapunovSum = 0.0;
    int stepCount = 0;
    
    // 3D visualization elements
    ODESolver ode_solver = new RK45MultiStep(this);
    Element ball1 = new ElementEllipsoid();
    Element ball2 = new ElementEllipsoid();
    ElementTrail trail1 = new ElementTrail();
    ElementTrail trail2 = new ElementTrail();

    /**
     * Constructor
     */
    public LorenzLyapunovParam() {
        // Setup main trajectory (red)
        ball1.setSizeXYZ(1, 1, 1);
        ball1.getStyle().setFillColor(Color.RED);
        trail1.getStyle().setLineColor(Color.RED);
        trail1.setMaximumPoints(5000);  // Note: This is for 3D trails, separate from plot data
        
        // Setup perturbed trajectory (blue, smaller and more transparent)
        ball2.setSizeXYZ(0.5, 0.5, 0.5);
        ball2.getStyle().setFillColor(new Color(0, 0, 255, 100));
        trail2.getStyle().setLineColor(new Color(0, 0, 255, 100));
        trail2.setMaximumPoints(5000);  // Note: This is for 3D trails, separate from plot data
        
        // Add elements to group
        addElement(trail1);
        addElement(trail2);
        addElement(ball1);
        addElement(ball2);
        
        ode_solver.setStepSize(0.01);
    }

    /**
     * Initialize the system with separate parameters for each trajectory
     */
    public void initialize(double x1, double y1, double z1, double x2, double y2, double z2,
                          double sigma1, double rho1, double beta1, 
                          double sigma2, double rho2, double beta2) {
        // Set parameters for both systems
        this.sigma1 = sigma1;
        this.rho1 = rho1;
        this.beta1 = beta1;
        this.sigma2 = sigma2;
        this.rho2 = rho2;
        this.beta2 = beta2;
        
        // Initialize trajectories
        state[0] = x1; // x1
        state[1] = y1; // y1
        state[2] = z1; // z1
        state[3] = x2; // x2
        state[4] = y2; // y2
        state[5] = z2; // z2
        state[6] = 0;  // time
        
        // Calculate initial separation
        double dx = state[3] - state[0];
        double dy = state[4] - state[1];
        double dz = state[5] - state[2];
        initialSeparation = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        // If initial conditions are identical, set a small separation
        if (initialSeparation < 1e-15) {
            initialSeparation = 1e-13; // Default small separation for parameter studies
        }
        
        // Reset Lyapunov calculation
        lyapunovSum = 0.0;
        stepCount = 0;
        
        // Clear trails and set initial positions
        trail1.clear();
        trail2.clear();
        trail1.addPoint(state[0], state[1], state[2]);
        trail2.addPoint(state[3], state[4], state[5]);
        ball1.setXYZ(state[0], state[1], state[2]);
        ball2.setXYZ(state[3], state[4], state[5]);
    }

    /**
     * Performs one integration step
     */
    protected void doStep() {
        ode_solver.step();
        
        // Calculate separation for Lyapunov
        double dx = state[3] - state[0];
        double dy = state[4] - state[1];
        double dz = state[5] - state[2];
        double separation = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        // Update Lyapunov sum
        if (separation > 0 && stepCount > 0) {
            lyapunovSum += Math.log(separation / initialSeparation);
            
            // Renormalize to prevent overflow
            double scale = initialSeparation / separation;
            state[3] = state[0] + dx * scale;
            state[4] = state[1] + dy * scale;
            state[5] = state[2] + dz * scale;
        }
        
        stepCount++;
        
        // Update visualization in state space
        trail1.addPoint(state[0], state[1], state[2]);
        trail2.addPoint(state[3], state[4], state[5]);
        ball1.setXYZ(state[0], state[1], state[2]);
        ball2.setXYZ(state[3], state[4], state[5]);
    }

    /**
     * Gets the current state array
     */
    public double[] getState() {
        return state;
    }

    /**
     * Calculates derivatives for ODE solver using separate parameters
     */
    public void getRate(double[] state, double[] rate) {
        // System 1 rates (using parameters sigma1, rho1, beta1)
        rate[0] = sigma1 * (state[1] - state[0]);           // dx1/dt
        rate[1] = state[0] * (rho1 - state[2]) - state[1];  // dy1/dt
        rate[2] = state[0] * state[1] - beta1 * state[2];   // dz1/dt
        
        // System 2 rates (using parameters sigma2, rho2, beta2)
        rate[3] = sigma2 * (state[4] - state[3]);           // dx2/dt
        rate[4] = state[3] * (rho2 - state[5]) - state[4];  // dy2/dt
        rate[5] = state[3] * state[4] - beta2 * state[5];   // dz2/dt
        
        rate[6] = 1; // time rate
    }
    
    /**
     * Gets current time
     */
    public double getTime() {
        return state[6];
    }
    
    /**
     * Gets current Lyapunov exponent
     */
    public double getCurrentLyapunov() {
        if (state[6] > 0) {
            return lyapunovSum / state[6];
        }
        return 0.0;
    }
}